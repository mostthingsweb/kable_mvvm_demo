package io.laplante.kmd_annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GenerateBoundServiceWrapper(val binder: KClass<*>)

