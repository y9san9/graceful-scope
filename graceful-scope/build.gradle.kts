plugins {
    id("kmp-library-convention")
    alias(libs.plugins.ktlint)
}

version = libs.versions.graceful.scope.get()

dependencies {
    commonMainImplementation(libs.coroutines)
}
