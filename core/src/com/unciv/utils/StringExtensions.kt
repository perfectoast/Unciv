package com.unciv.utils

import java.util.UUID

/**
 * Checks if a [String] is a valid UUID
 */
fun String.isUUID(): Boolean = try {
    UUID.fromString(this)
    true
} catch (_: Throwable) {
    false
}
