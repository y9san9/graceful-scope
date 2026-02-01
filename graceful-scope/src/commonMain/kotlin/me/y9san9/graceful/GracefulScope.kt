@file:OptIn(ExperimentalAtomicApi::class)

package me.y9san9.graceful

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.time.Duration

public suspend inline fun <R> gracefulScope(
    crossinline block: suspend (GracefulScope) -> R,
): R = coroutineScope {
    val scope = GracefulScope(scope = this)
    block(scope)
}

/**
 * GracefulScope intentionally avoids inheritance from [CoroutineScope], to
 * leave the API surface limited and remove access from APIs that may be
 * wrongfully used, like cancel() function with the expectation of graceful
 * cancellation.
 */
public class GracefulScope(private val scope: CoroutineScope) {
    private val isActive = AtomicBoolean(true)

    /**
     * Launches a new job in [scope] if it is still active. Otherwise, does
     * nothing.
     *
     * This intentionally does not affect any jobs that are launched inside
     * [block], so you can create subtasks for already running tasks.
     */
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        // Double-check lock.
        // Explanation for future maintainers, NOT AI comment, lol.
        // If this scope is not active, does not start a job.
        if (!isActive.load()) {
            val result = Job(parent = scope.coroutineContext.job)
            result.cancel()
            return result
        }
        // Race-window, stop() function may check for isActive and call
        // job.joinChildren().
        return scope.launch(context, start) {
            // We caught race-condition and recover from it by cancelling this
            // job. Without 'if' here, stop() function could've skipped this
            // job and let it to be executed.
            if (!isActive.load()) {
                throw CancellationException("GracefulScope is not active")
            }
            block()
        }
    }

    /**
     * Launches a new job in [scope] if it is still active. Otherwise,
     * does nothing.
     *
     * This intentionally does not affect any jobs that are launched inside
     * [block], so you can create subtasks for already running tasks.
     */
    public fun <T> async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T,
    ): Deferred<T> {
        // Double-check lock.
        // Explanation for future maintainers, NOT AI comment, lol.
        // If this scope is not active, does not start a job.
        if (!isActive.load()) {
            val deferred = CompletableDeferred<T>(
                parent = scope.coroutineContext.job,
            )
            deferred.cancel()
            return deferred
        }
        // Race-window, stop() function may check for isActive and call
        // job.joinChildren().
        return scope.async(context, start) {
            // We caught race-condition and recover from it by cancelling this
            // job. Without 'if' here, stop() function could've skipped this
            // job and let it to be executed.
            if (!isActive.load()) {
                throw CancellationException("GracefulScope is not active")
            }
            block()
        }
    }

    /**
     * Switches execution to a graceful scope. It is similar to
     * withContext(NonCancellable) in terms that GracefulScope will try to wait
     * until this job is finished
     */
    public suspend fun <T> with(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> T,
    ): T {
        val deferred = async(context, CoroutineStart.UNDISPATCHED, block)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { deferred.cancel() }
            deferred::await.startCoroutine(continuation)
        }
    }

    /**
     * Tries to stop this scope in a graceful way.
     *
     * First phase:
     *
     * * Forbids new jobs to be started on this scope. However, new jobs still
     *   can be started inside already running jobs attached to this scope.
     *   This phase ends after [cooldownTimeout] or when all children are
     *   completed.
     *
     * Second phase:
     *
     * * Starts cancellation of existing jobs using coroutines cancellation
     *   mechanism. This phase ends after [cancellationTimeout] or when all
     *   children are cancelled.
     *
     * Then, function returns true or false depending on whether it could stop
     * running jobs in that timeout. If it returns false, cancellation process
     * continues afterwards.
     */
    public suspend fun stop(
        cooldownTimeout: Duration? = null,
        cancellationTimeout: Duration? = null,
    ): Boolean {
        isActive.store(false)
        if (cooldownTimeout != null) {
            withTimeoutOrNull(cooldownTimeout) {
                scope.coroutineContext.job.joinChildren()
            }
        }
        if (cancellationTimeout == null) {
            scope.coroutineContext.job.cancelAndJoin()
            return true
        }
        val result = withTimeoutOrNull(cancellationTimeout) {
            scope.coroutineContext.job.cancelAndJoin()
        }
        return result != null
    }

    private suspend fun Job.joinChildren() {
        while (true) {
            val job = children.firstOrNull() ?: break
            job.join()
        }
    }
}
