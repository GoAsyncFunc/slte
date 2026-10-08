package com.slte.app.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.component.SlteCard
import com.slte.app.ui.component.SlteGroupDivider
import com.slte.app.ui.component.SlteRow
import com.slte.app.ui.component.rememberSinkScale
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

@Composable
internal fun UserInfoCard(
    email: String,
    balance: String,
) {
    SlteCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                icon = SlteIcons.Email,
                text = "${stringResource(R.string.profile_email_label)} ${email.ifBlank { stringResource(R.string.profile_not_logged_in) }}",
            )
            SlteGroupDivider()
            InfoRow(
                icon = SlteIcons.Balance,
                text =
                "${stringResource(R.string.purchase_balance)} " +
                    stringResource(R.string.currency_symbol) + balance,
            )
        }
    }
}

@Composable
internal fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(Dimens.size.row)
            .padding(horizontal = Dimens.gap.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.icon.lg),

            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(Dimens.gap.md))
        Text(
            text = text,
            style = SlteType.title,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun NavigateRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) = SlteRow(
    icon = icon,
    title = title,
    chevron = true,
    onClick = onClick,
)

@Composable
internal fun LogoutCard(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val sink = rememberSinkScale(interactionSource)
    SlteCard(
        modifier =
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = sink.value
                scaleY = sink.value
            }.clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimens.size.row)
                .padding(horizontal = Dimens.gap.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = SlteIcons.Logout,
                contentDescription = null,
                modifier = Modifier.size(Dimens.icon.lg),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(Dimens.gap.md))
            Text(
                text = stringResource(R.string.profile_logout),
                style = SlteType.title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
