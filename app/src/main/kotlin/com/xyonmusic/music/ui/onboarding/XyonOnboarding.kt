/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * First-run onboarding: two calm steps in XyonMusic's own style. The glass headphones logo floats
 * over a slow colour field, then a single permissions card, and in. Buttons are native liquid glass.
 */

package com.xyonmusic.music.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xyonmusic.music.BuildConfig
import com.xyonmusic.music.R
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.liquidGlass
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF07080C)

@Composable
fun XyonOnboarding(onFinished: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step = 0 }

    fun notifGranted() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val needsInstall = BuildConfig.FLAVOR.contains("gms") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun installGranted() = !needsInstall || runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
    var notif by remember { mutableStateOf(notifGranted()) }
    var install by remember { mutableStateOf(installGranted()) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { notif = notifGranted(); install = installGranted() } }
        lifecycle.lifecycle.addObserver(o)
        onDispose { lifecycle.lifecycle.removeObserver(o) }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notif = it }

    val backdrop = rememberGlassBackdrop()
    Box(Modifier.fillMaxSize().background(Ink)) {
        Box(Modifier.fillMaxSize().glassSource(backdrop)) { Aurora() }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(420, delayMillis = 120)) + scaleIn(tween(520, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                    fadeOut(tween(200))
            },
            label = "onboardingStep",
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
        ) { s ->
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (s == 0) {
                    Spacer(Modifier.weight(1f))
                    FloatingLogo()
                    Spacer(Modifier.height(36.dp))
                    Text("XyonMusic", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onb_tagline), color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1.2f))
                    GlassButton(stringResource(R.string.onb_start), backdrop, primary = true) {
                        if (notif && install) onFinished() else step = 1
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    Image(
                        painterResource(R.drawable.xyon_glass_logo), null,
                        Modifier.size(84.dp).shadow(24.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.onb_perm_title), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(24.dp))
                    PermissionRow(stringResource(R.string.onb_perm_body), notif, backdrop) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (needsInstall) {
                        Spacer(Modifier.height(12.dp))
                        PermissionRow(stringResource(R.string.onb_install_body), install, backdrop) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1.2f))
                    GlassButton(stringResource(R.string.onb_done), backdrop, primary = true, onClick = onFinished)
                }
            }
        }
    }
}

/** The glass headphones logo, floating gently with a soft halo behind it. */
@Composable
private fun FloatingLogo() {
    val t by rememberInfiniteTransition(label = "logo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "logoT",
    )
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp)) {
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)), radius = size.minDimension / 2f)
        }
        Image(
            painterResource(R.drawable.xyon_glass_logo), null,
            Modifier
                .graphicsLayer {
                    val a = t * 2f * Math.PI.toFloat()
                    translationY = sin(a) * 6.dp.toPx()
                    rotationZ = sin(a) * 1.5f
                }
                .size(150.dp)
                .shadow(36.dp, RoundedCornerShape(34.dp), ambientColor = Color.White, spotColor = Color.White)
                .clip(RoundedCornerShape(34.dp)),
        )
    }
}

@Composable
private fun PermissionRow(text: String, granted: Boolean, backdrop: GlassBackdrop, onAllow: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .liquidGlass(backdrop, shape, LiquidGlassStyle.Dark)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        val label = stringResource(if (granted) R.string.onb_allowed else R.string.onb_allow)
        Box(
            Modifier
                .clip(CircleShape)
                .background(if (granted) Color.White.copy(alpha = 0.12f) else Color.White)
                .clickable(enabled = !granted, onClick = onAllow)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Text(label, color = if (granted) Color.White.copy(alpha = 0.7f) else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GlassButton(text: String, backdrop: GlassBackdrop, primary: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "btn")
    Box(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .liquidGlass(backdrop, CircleShape, LiquidGlassStyle.Dark, tint = if (primary) Color.White.copy(alpha = 0.22f) else Color.Unspecified)
            .clip(CircleShape)
            .clickable(interaction, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Slow drifting colour field behind the onboarding; the glass refracts it. */
@Composable
private fun Aurora() {
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart), label = "auroraT",
    )
    val blobs = remember { listOf(Color(0xFF4C5BFF), Color(0xFFFF3D7F), Color(0xFF00D1C1), Color(0xFF8A3BFF)) }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Ink)
        val w = size.width; val h = size.height
        blobs.forEachIndexed { i, c ->
            val ph = t + i * 1.6f
            val center = Offset(w * (0.5f + 0.4f * sin(ph * (1 + i * 0.1f))), h * (0.45f + 0.35f * cos(ph * 0.8f + i)))
            val r = maxOf(w, h) * (if (i % 2 == 0) 0.42f else 0.32f)
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.42f), c.copy(alpha = 0.10f), Color.Transparent), center, r), r, center)
        }
        drawRect(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.25f), Color.Transparent, Ink.copy(alpha = 0.7f))))
    }
}
