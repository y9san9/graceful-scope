plugins {
    id("print-version-convention")
    alias(libs.plugins.ktlint)
}

version = libs.versions.graceful.scope.get()
