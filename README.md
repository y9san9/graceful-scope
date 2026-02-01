# Graceful Scope

This library provides a single class called `GracefulScope`. It supports running
coroutine jobs like any `CoroutineScope`, but it also has a support of graceful
stop. Graceful stop happens in two phases:

* **First phase**: Forbids new jobs to be started on this scope. However,
  new jobs still can be started inside already running jobs attached to this
  scope. This phase ends after `cooldownTimeout` or when all children are
  completed.

* **Second phase**: Starts cancellation of existing jobs using coroutines
  cancellation mechanism. This phase ends after `cancellationTimeout` or when
  all children are cancelled.

Then, `stop()` function returns true or false depending on whether it could stop
running jobs in `cancellationTimeout`. If it returns false, cancellation process
never stops, but control flow is returned back to the user of API.

## Install

Replace $version with the latest version from `Releases` Tab.

```kotlin
dependencies {
    implementation("me.y9san9.graceful:graceful-scope:$version")
}
```

## Example

See [this example](example/src/main/kotlin/Main.kt) to play around with
Graceful Scope.

## Shutdown Hook

Usual pattern of using such a scope is together with Shutdown Hook:

```kotlin
gracefulScope { notificationsScope ->
    runNotificationsActor(notificationsScope)

    Runtime.getRuntime().addShutdownHook {
        notificationsScope.stop(
            cooldownTimeout = 15.seconds,
            cancellationTimeout = null,
        )
    }
}
```

This will ensure that when you restart service, notifications will have a
15-seconds chance to finish currently running jobs, but no new jobs will be
accepted. It provides the same functionality that `server.stop()` from ktor
does, but in a more general way.

