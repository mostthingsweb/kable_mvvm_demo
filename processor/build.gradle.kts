import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    idea
}

idea {
    module {
        isDownloadSources = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
}

dependencies {
    implementation(project(":annotations"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")

    implementation("com.squareup:kotlinpoet:1.9.0")
    implementation("com.squareup:kotlinpoet-metadata:1.9.0")
    implementation("com.squareup:kotlinpoet-metadata-specs:1.9.0")

    annotationProcessor("com.google.auto.service:auto-service:1.0")
    implementation("com.google.auto.service:auto-service:1.0")
    kapt("com.google.auto.service:auto-service:1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
}