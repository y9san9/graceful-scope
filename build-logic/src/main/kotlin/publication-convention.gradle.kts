import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

group = "me.y9san9.graceful"

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    pom {
        name = "graceful-scope"
        description = "Graceful stop support for kotlinx.coroutines"
        url = "https://github.com/y9san9/graceful-scope"

        licenses {
            license {
                name = "MIT"
                distribution = "repo"
                url = "https://github.com/y9san9/graceful/blob/main/LICENSE.md"
            }
        }

        developers {
            developer {
                id = "y9san9"
                name = "Alex Sokol"
                email = "y9san9@gmail.com"
            }
        }

        scm {
            connection ="scm:git:ssh://github.com/y9san9/graceful-scope.git"
            developerConnection = "scm:git:ssh://github.com/y9san9/graceful-scope.git"
            url = "https://github.com/y9san9/graceful-scope"
        }
    }

    signAllPublications()
}
