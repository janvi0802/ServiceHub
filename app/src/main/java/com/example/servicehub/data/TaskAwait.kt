package com.example.servicehub.data

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase answers with a "Task" that finishes later (like a promise).
 * This helper lets us simply write `val result = task.await()` inside a coroutine:
 * the coroutine pauses until Firebase answers, then continues with the result
 * (or throws the error, which we catch with try/catch).
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Something went wrong"))
        }
    }
}
