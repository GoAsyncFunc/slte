package com.slte.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.slte.app.domain.repository.LocaleRepository
import com.slte.app.utils.LocaleContextWrapper
import java.util.Locale

val LocalAppLocale = staticCompositionLocalOf<Locale?> { null }

val LocalLocaleRepository = staticCompositionLocalOf<LocaleRepository?> { null }

@Composable
fun AppLocaleContent(
    locale: Locale?,
    localeRepository: LocaleRepository? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val repository = localeRepository ?: LocalLocaleRepository.current
    val localeContext =
        remember(locale) {
            LocaleContextWrapper(context) { repository?.locale?.value ?: locale }
        }
    CompositionLocalProvider(
        LocalContext provides localeContext,
        LocalAppLocale provides locale,
        LocalLocaleRepository provides repository,
    ) {
        content()
    }
}

@Composable
fun LocaleAwareAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            AppLocaleContent(locale = LocalAppLocale.current) { confirmButton() }
        },
        modifier = modifier,
        dismissButton =
        dismissButton?.let { d ->
            { AppLocaleContent(locale = LocalAppLocale.current) { d() } }
        },
        icon =
        icon?.let { i ->
            { AppLocaleContent(locale = LocalAppLocale.current) { i() } }
        },
        title =
        title?.let { t ->
            { AppLocaleContent(locale = LocalAppLocale.current) { t() } }
        },
        text =
        text?.let { t ->
            { AppLocaleContent(locale = LocalAppLocale.current) { t() } }
        },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}
