plugins {
    kotlin("jvm")
    application
}

application {
    mainClass = "MainKt"
}

dependencies {
    implementation(libs.coroutines)
    implementation(projects.gracefulScope)
}
