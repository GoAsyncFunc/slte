package com.slte.app.ui.screen.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import com.slte.app.R
import com.slte.app.ui.component.CircleIconButton
import com.slte.app.ui.component.UsageCard
import com.slte.app.ui.component.rememberToast
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    mainViewModel: MainViewModel,
    data: DashboardData,
    onInvite: () -> Unit = {},
    onServer: () -> Unit = {},
    onNotice: () -> Unit = {},
    onSupport: () -> Unit = {},
    onProfile: () -> Unit = {},
    onRenew: () -> Unit = {},
) {
    val context = LocalContext.current
    val toast = rememberToast()
    var showProxySheet by remember { mutableStateOf(false) }
    val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                mainViewModel.toggleConnection()
            } else {
                mainViewModel.onVpnPermissionDenied()
            }
        }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }

    LaunchedEffect(Unit) {
        mainViewModel.refreshKernelInfo()
    }

    // 首页可见且已开通套餐时，持续跟随内核的实时落点节点（连接与否都要跟随）
    LaunchedEffect(data.hasPlan) {
        if (data.hasPlan) {
            mainViewModel.watchLiveSelection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = SlteType.title,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    CircleIconButton(
                        icon = SlteIcons.Support,
                        description = stringResource(R.string.topbar_support),
                        onClick = onSupport,
                    )
                    CircleIconButton(
                        icon = SlteIcons.Notifications,
                        description = stringResource(R.string.topbar_notice),
                        onClick = onNotice,
                    )
                    CircleIconButton(
                        icon = SlteIcons.Profile,
                        description = stringResource(R.string.topbar_profile),
                        onClick = onProfile,
                    )
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        DashboardContent(
            data = data,
            onToggleConnection = {
                if (!data.hasPlan) {
                    toast.show(R.string.dashboard_no_plan_tip)
                    onRenew()
                } else {
                    requestNotificationPermission(context, notificationPermissionLauncher)
                    val request = mainViewModel.vpnRequestIntent()
                    if (request != null) {
                        vpnPermissionLauncher.launch(request)
                    } else {
                        mainViewModel.toggleConnection()
                    }
                }
            },
            onServerClick = onServer,
            onProxyClick = { showProxySheet = true },
            onUpdateSubscription = {
                if (data.hasPlan) {
                    mainViewModel.updateSubscription()
                } else {
                    onRenew()
                }
            },
            onInvite = onInvite,
            onRenew = onRenew,
            modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }

    if (showProxySheet) {
        ProxyModeSheet(
            currentMode = data.proxyMode,
            onDismiss = { showProxySheet = false },
            onSelect = mainViewModel::setProxyMode,
        )
    }
}

private fun requestNotificationPermission(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
internal fun DashboardContent(
    data: DashboardData,
    onToggleConnection: () -> Unit,
    onServerClick: () -> Unit,
    onProxyClick: () -> Unit,
    onUpdateSubscription: () -> Unit,
    onInvite: () -> Unit,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        val compact = maxHeight < Dimens.dashboardCompactBreakpoint
        val cardSpacing = if (compact) Dimens.dashboardCardSpacingCompact else Dimens.dashboardCardSpacing
        val screenPaddingV = if (compact) Dimens.dashboardScreenPaddingVCompact else Dimens.dashboardScreenPaddingV
        val toggleMinHeight =
            if (compact) Dimens.dashboardToggleCardMinHeightCompact else Dimens.dashboardToggleCardMinHeight

        // 上方三块内容（套餐卡 / 信息列表 / 双按钮）的实测高度：只有知道它们占了多少，
        // 才能让底部连接卡精确补满剩下的空间——卡片上沿贴紧按钮、下沿贴紧安全区底部，
        // 任何机型都落在同一位置：既不会探进系统小白条，也不会在下方留出大小不一的空白。
        // 空间不够时（小屏 / 大字体）退到最小高度并允许整屏滚动，避免卡片被压扁。
        val density = LocalDensity.current
        var fixedContentHeight by remember { mutableStateOf<Dp?>(null) }
        val toggleHeight =
            fixedContentHeight
                ?.let { (maxHeight - screenPaddingV * 2 - cardSpacing - it).coerceAtLeast(toggleMinHeight) }
                ?: toggleMinHeight

        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.dashboardScreenPaddingH)
                .padding(vertical = screenPaddingV),
            verticalArrangement = Arrangement.spacedBy(cardSpacing),
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fixedContentHeight = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
                UsageCard(
                    planName = data.planName,
                    usedBytes = data.usedBytes,
                    totalBytes = data.totalBytes,
                    isValid = data.isValid,
                    hasPlan = data.hasPlan,
                    daysUntilExpired = if (data.expiredAt > 0L) data.daysUntilExpired else null,
                    expiredAtDate = if (data.expiredAt > 0L) FormatUtils.formatExpiryDate(data.expiredAt) else null,
                    actionText =
                    stringResource(
                        if (data.hasPlan) {
                            R.string.plan_renew_button
                        } else {
                            R.string.plan_buy_button
                        },
                    ),
                    actionEnabled = true,
                    onAction = onRenew,
                )
                InfoListCard(
                    daysUntilExpired = if (data.expiredAt > 0L) data.daysUntilExpired else null,
                    serverName = data.serverName,
                    proxyMode = data.proxyMode,
                    currentIp = data.currentIp,
                    ipCountryCode = data.ipCountryCode,
                    onServerClick = onServerClick,
                    onProxyClick = onProxyClick,
                )
                DashboardActionButtons(
                    onUpdateSubscription = onUpdateSubscription,
                    hasPlan = data.hasPlan,
                    onInvite = onInvite,
                )
            }

            ConnectToggleCard(
                isConnected = data.isConnected,
                isConnecting = data.isConnecting,
                onToggle = onToggleConnection,
                modifier = Modifier.height(toggleHeight),
                minHeight = toggleMinHeight,
            )
        }
    }
}
