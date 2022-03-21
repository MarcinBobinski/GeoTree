package com.example.geotreeapp.common

import kotlinx.coroutines.delay
import timber.log.Timber

suspend fun <T> retrySuspend(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000,    // 1 second
    factor: Double = 2.0,
    block: suspend () -> T): T
{
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            Timber.e(e)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

fun <T> retry(
    times: Int = 3,
    block: () -> T): T
{
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    return block() // last attempt
}