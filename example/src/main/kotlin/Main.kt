import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlin.time.measureTime
import me.y9san9.graceful.gracefulScope
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

// !!! This example should not be launched by gradle (or IDEA) !!!
// Gradle kills tasks without any respect to shutdown hooks
// First, install it using ./gradlew installDist, then launch binary
suspend fun main() = gracefulScope { scope ->
    println("Started Main.kt")
    val hook = Thread {
        runBlocking { scope.stop(15.seconds) }
    }
    Runtime.getRuntime().addShutdownHook(hook)
    scope.with {
        println("Enter Ctrl-C now, this job will finish anyways (5 seconds)...")
        delay(5_000)
        println("Hello World!")
    }
    scope.with {
        println("And this will not, since it is launched after Ctrl-C")
    }
}
