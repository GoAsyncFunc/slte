package com.slte.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

private const val STICKER_FPS = 30

@Composable
fun AnimatedSticker(
    assetPath: String,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset(assetPath),
    )
    val rawProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )
    val progress = remember {
        derivedStateOf {
            val composition = composition ?: return@derivedStateOf rawProgress
            val steps =
                (composition.durationFrames * STICKER_FPS / composition.frameRate).coerceAtLeast(1f)
            (rawProgress * steps).toInt() / steps
        }
    }
    LottieAnimation(
        composition = composition,
        progress = { progress.value },
        modifier = modifier,
    )
}
