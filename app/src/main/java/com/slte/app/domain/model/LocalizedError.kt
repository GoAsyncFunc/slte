package com.slte.app.domain.model

/** Error carrying an optional app string resource for user-facing presentation. */
interface LocalizedError {
    val stringResId: Int?
}
