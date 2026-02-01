enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "graceful-scope-root"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

includeBuild("build-logic")

include("graceful-scope", "example")
