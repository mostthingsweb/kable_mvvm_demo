package io.laplante.kmd_processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import io.laplante.kmd_annotations.GenerateBoundServiceWrapper
import kotlinx.metadata.KmClassifier
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

// https://stackoverflow.com/a/58448607/221061
inline fun <reified T : Annotation> Element.getAnnotationClassValue(f: T.() -> KClass<*>) = try {
    getAnnotation(T::class.java).f()
    throw Exception("Expected to get a MirroredTypeException")
} catch (e: MirroredTypeException) {
    e.typeMirror
}

fun Element.createClassName(): ClassName {
    val typeMetadata = getAnnotation(Metadata::class.java)
    val kmClass = typeMetadata.toImmutableKmClass()
    return ClassInspectorUtil.createClassName(kmClass.name)
}

fun ImmutableKmType.classifierClassName(): ClassName =
    ClassInspectorUtil.createClassName((this.classifier as KmClassifier.Class).name)


private fun TypeSpec.Builder.generatePropertiesForStateFlow(property: ImmutableKmProperty) {
    val argumentClassName = property.returnType.arguments.first().type!!.classifierClassName()
    val isList = argumentClassName == List::class.asTypeName()

    fun ClassName.recursiveParameterizedBy(
        argumentTypes: List<ImmutableKmTypeProjection>,
        level: Int = 0
    ): TypeName {
        if (argumentTypes.isEmpty()) {
            return this.copy()
        }

        val w = argumentTypes
            .map {
                val type = it.type!!
                val baseClassName = it.type!!.classifierClassName()
                    .copy(nullable = (level == 0)) as ClassName

                baseClassName.recursiveParameterizedBy(type.arguments, level + 1)
            }

        val isOutermostType = level == 1
        return this.parameterizedBy(w).copy(nullable = isOutermostType && !isList)
    }

    val mutableStateFlowType =
        ForeignDeps.kotlinx.coroutines.flow.MutableStateFlow.recursiveParameterizedBy(property.returnType.arguments)

    val publicStateFlowType =
        ForeignDeps.kotlinx.coroutines.flow.StateFlow.recursiveParameterizedBy(property.returnType.arguments)

    val builder = PropertySpec
        .builder("_${property.name}", mutableStateFlowType)
        .addModifiers(KModifier.PRIVATE)
    if (isList) {
        builder.initializer("MutableStateFlow(emptyList())")
    } else {
        builder.initializer("MutableStateFlow(%L)", null)
    }

    val privatePropertySpec = builder.build()
    addProperty(privatePropertySpec)

    addProperty(
        PropertySpec
            .builder(property.name, publicStateFlowType)
            .initializer(
                "%N.%M()",
                privatePropertySpec,
                ForeignDeps.kotlinx.coroutines.flow.asStateFlow
            )
            .build()
    )
}

fun createServiceConnectionObject(
    binderClass: ClassName,
    generatedProperties: List<ImmutableKmProperty>,
    wrapperClassName: String
): TypeSpec {
    return TypeSpec.anonymousClassBuilder()
        .addSuperinterface(ForeignDeps.android.content.ServiceConnection)
        // override fun onServiceConnected(className: ComponentName, service: IBinder)
        .addFunction(
            FunSpec.builder("onServiceConnected")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("className", ForeignDeps.android.content.ComponentName)
                .addParameter("service", ForeignDeps.android.os.IBinder)
                .addCode(buildCodeBlock {
                    addStatement("val binder = service as %T", binderClass)
                    addStatement("_service = binder.getService()")
                    addStatement("_bound = true")
                    addStatement("this@%N.onServiceConnected(_service)", wrapperClassName)

                    addStatement(
                        "_service.%M.%M {",
                        ForeignDeps.androidx.lifecycle.LifecycleScope,
                        ForeignDeps.kotlinx.coroutines.launch
                    )
                    withIndent {
                        for (property in generatedProperties) {
                            addStatement("%M {", ForeignDeps.kotlinx.coroutines.launch)
                            withIndent {
                                addStatement(
                                    "_%N.%M(_service.%N)",
                                    property.name,
                                    ForeignDeps.kotlinx.coroutines.flow.emitAll,
                                    property.name
                                )
                            }
                            addStatement("}")
                        }
                    }
                    addStatement("}")
                })
                .build()
        )
        // override fun onServiceDisconnected(className: ComponentName)
        .addFunction(
            FunSpec.builder("onServiceDisconnected")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("className", ForeignDeps.android.content.ComponentName)
                .addCode("_bound = false")
                .build()
        )
        .build()
}

@AutoService(Processor::class)
@SupportedOptions(GenerateBoundServiceWrapperAnnotationProcessor.KAPT_KOTLIN_GENERATED_DIRECTORY_NAME)
class GenerateBoundServiceWrapperAnnotationProcessor : AbstractProcessor() {
    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

    private val targetDirectory: String
        get() = processingEnv.options[KAPT_KOTLIN_GENERATED_DIRECTORY_NAME]
            ?: throw IllegalStateException("Unable to get target directory")

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(GenerateBoundServiceWrapper::class.qualifiedName!!)
    }

    private fun TypeMirror.createClassName(): ClassName =
        processingEnv.typeUtils.asElement(this).createClassName()

    private fun createWrapperClass(
        serviceClassElement: TypeElement,
        wrapperClassName: String
    ): TypeSpec {
        val binderClassName =
            serviceClassElement.getAnnotationClassValue<GenerateBoundServiceWrapper> { binder }
                .createClassName()

        val wrapperClassBuilder = TypeSpec
            .classBuilder(wrapperClassName)
            .addModifiers(KModifier.OPEN)

        val serviceClassMetadata =
            serviceClassElement.getAnnotation(Metadata::class.java).toImmutableKmClass()

        val generatedProperties = serviceClassMetadata.properties
            .mapNotNull {
                when (it.isPrivate) {
                    true -> null
                    false -> {
                        when (it.returnType.classifierClassName()) {
                            ForeignDeps.kotlinx.coroutines.flow.StateFlow, ForeignDeps.kotlinx.coroutines.flow.Flow -> {
                                wrapperClassBuilder.generatePropertiesForStateFlow(it)
                                it
                            }
                            else -> null
                        }
                    }
                }
            }

        val wrapperClassCtorBuilder = FunSpec.constructorBuilder()
            .addParameter("applicationContext", ForeignDeps.android.content.Context)

        wrapperClassBuilder.addAnnotation(ForeignDeps.javax.inject.Singleton)
            .primaryConstructor(wrapperClassCtorBuilder.build())
            .addSuperinterface(ForeignDeps.androidx.lifecycle.LifecycleObserver)
            .addProperty(
                PropertySpec.builder(
                    "applicationContext",
                    ForeignDeps.android.content.Context
                )
                    .initializer("applicationContext")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("_service", serviceClassElement.createClassName())
                    .addModifiers(KModifier.LATEINIT, KModifier.PROTECTED)
                    .mutable()
                    .build()
            )
            .addProperty(
                PropertySpec.builder("_bound", Boolean::class)
                    .addModifiers(KModifier.PRIVATE)
                    .mutable()
                    .initializer("%L", false)
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "_connection",
                    ForeignDeps.android.content.ServiceConnection
                )
                    .initializer(
                        "%L",
                        createServiceConnectionObject(binderClassName, generatedProperties, wrapperClassName)
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("handleLifecycleStart").addCode(
                    buildCodeBlock {
                        addStatement(
                            "%T(applicationContext, %T::class.java).also { intent ->",
                            ForeignDeps.android.content.Intent,
                            serviceClassElement
                        )
                        withIndent {
                            addStatement("applicationContext.bindService(intent, _connection, Context.BIND_AUTO_CREATE)")
                        }
                        addStatement("}")
                    })
                    .addAnnotation(
                        AnnotationSpec.builder(ForeignDeps.androidx.lifecycle.OnLifecycleEvent)
                            .addMember(
                                "%T.Event.ON_START",
                                ForeignDeps.androidx.lifecycle.Lifecycle
                            )
                            .build()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("onServiceConnected")
                    .addModifiers(KModifier.OPEN)
                    .addParameter("service", serviceClassElement.createClassName())
                    .build()
            )

        return wrapperClassBuilder.build()
    }

    override fun process(p0: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv
            ?.getElementsAnnotatedWith(GenerateBoundServiceWrapper::class.java)
            ?.forEach { serviceClassElement ->
                check(serviceClassElement.kind.isClass && serviceClassElement is TypeElement)

                val serviceName = serviceClassElement.simpleName
                val wrapperClassName = "${serviceName}WrapperBase"

                val wrapperClass = createWrapperClass(serviceClassElement, wrapperClassName)

                val wrapperFileSpec = FileSpec
                    .builder("io.laplante.kmd.generated", wrapperClassName)
                    .addType(wrapperClass)
                    .build()

                wrapperFileSpec.writeTo(File(targetDirectory))
            }
        return true
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_DIRECTORY_NAME = "kapt.kotlin.generated"
    }
}

