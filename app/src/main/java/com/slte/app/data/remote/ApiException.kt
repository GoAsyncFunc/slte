package com.slte.app.data.remote

import androidx.annotation.StringRes
import com.slte.app.domain.model.LocalizedError
import java.io.IOException

class ApiException(
    override val message: String,
    @StringRes override val stringResId: Int? = null,
) : IOException(message),
    LocalizedError
