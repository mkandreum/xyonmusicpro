package com.xyonmusic.music.ui.feature

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xyonmusic.music.LocalDatabase
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.extensions.toMediaItem
import com.xyonmusic.music.models.toMediaMetadata
import com.xyonmusic.music.playback.queues.ListQueue
import com.xyonmusic.music.viewmodels.AiDjSetViewModel
import com.xyonmusic.music.viewmodels.JamViewModel
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.ui.LiquidGlassButton
import io.github.mkandreum.liquidbar.ui.LiquidPortalSheet
import io.github.mkandreum.liquidbar.ui.LiquidSegmented
import io.github.mkandreum.liquidbar.ui.LiquidSegmentedDefaults
import kotlinx.coroutines.flow.first

/** Which feature sheet is open. Any screen opens one by flipping its flag; the root draws it. */
object FeatureSheets {
    var juntos by mutableStateOf(false)
    var dj by mutableStateOf(false)
    val anyOpen: Boolean get() = juntos || dj
}

private val Accent = Color(0xFFFA2D48)
private val Muted = Color.White.copy(alpha = 0.6f)

/** Draws whichever feature sheet is open, as LiquidUI glass sheets through the root portal. */
@Composable
fun FeatureSheetsHost() {
    if (FeatureSheets.juntos) JuntosSheet(onDismiss = { FeatureSheets.juntos = false })
    if (FeatureSheets.dj) DjSheet(onDismiss = { FeatureSheets.dj = false })
    // Back closes the glass sheet before it reaches the player/screen underneath.
    BackHandler(enabled = FeatureSheets.anyOpen) {
        FeatureSheets.juntos = false
        FeatureSheets.dj = false
    }
}

@Composable
private fun SectionLabel(text: String) {
    androidx.compose.material3.Text(
        text.uppercase(),
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

/**
 * A pill button in LiquidUI glass. It refracts a soft tinted plate drawn right behind it (its own
 * small backdrop), so it reads as glass inside the sheet without nesting glass in glass.
 */
@Composable
private fun GlassPill(text: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val backdrop = rememberGlassBackdrop()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(modifier.height(52.dp)) {
        Box(
            Modifier.matchParentSize().glassSource(backdrop)
                .background(if (primary) Accent else Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp)),
        )
        LiquidGlassButton(
            onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm); onClick() },
            backdrop = backdrop,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(26.dp),
            tint = if (primary) Accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
        ) {
            androidx.compose.material3.Text(text, color = Color.White, fontSize = 15.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold)
        }
    }
}

/** Escuchar juntos: create a room, join one with a code, or see who is listening and leave. */
@Composable
private fun JuntosSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: JamViewModel = hiltViewModel()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var joining by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.warm() }

    LiquidPortalSheet(onDismiss = onDismiss, title = "Escuchar juntos", detents = listOf(0.62f, 0.9f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            androidx.compose.material3.Text(
                "Todos oís lo mismo a la vez. Cualquiera puede añadir canciones a la cola.",
                color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp, lineHeight = 20.sp,
            )
            val s = session
            if (s == null) {
                if (joining) {
                    SectionLabel("Código de la sala")
                    BasicTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit).take(8) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp),
                        cursorBrush = SolidColor(Accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassPill("Cancelar", Modifier.weight(1f)) { joining = false }
                        GlassPill(if (busy) "Entrando…" else "Unirme", Modifier.weight(1f), primary = true) {
                            if (code.length >= 4 && !busy) viewModel.join(code, "Yo")
                        }
                    }
                } else {
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassPill("Unirme con código", Modifier.weight(1f)) { joining = true }
                        GlassPill(if (busy) "Creando…" else "Crear sala", Modifier.weight(1f), primary = true) {
                            if (!busy) viewModel.create("Yo")
                        }
                    }
                }
                message?.let {
                    androidx.compose.material3.Text(it, color = Color(0xFFFF8494), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                }
            } else {
                SectionLabel("Código de la sala")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                ) {
                    androidx.compose.material3.Text(
                        s.code,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    GlassPill("Compartir", Modifier.width(128.dp)) {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "Escucha conmigo en XyonMusic: código ${s.code}\n${viewModel.inviteLink(s.code)}")
                        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                SectionLabel("En la sala · ${s.members.size}")
                s.members.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(memberColor(m.memberId)),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Text(m.displayName.take(1).uppercase().ifBlank { "·" }, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            if (m.memberId == s.you.memberId) "Tú" else m.displayName.ifBlank { "Invitado" },
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                        )
                        if (m.isHost) androidx.compose.material3.Text(
                            "Anfitrión", color = Color(0xFFFF8494), fontSize = 12.sp,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Accent.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlassPill("Salir de la sala", Modifier.fillMaxWidth(), primary = true) { viewModel.leave() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun memberColor(id: String): Color {
    val palette = listOf(0xFFE8A87C, 0xFF7CB2E8, 0xFFB07CE8, 0xFF7CE8B0, 0xFFE87C9C)
    return Color(palette[(id.hashCode() and 0x7fffffff) % palette.size])
}

/** DJ con IA: a continuous mix seeded from what you play most; the mood guides it when an AI key is set. */
@Composable
private fun DjSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AiDjSetViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val moods = listOf("Fiesta", "Relax", "Entreno", "Cena")
    val durations = listOf("30 min", "1 hora", "2 horas")
    var mood by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(1) }
    var seedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.reset()
        val month = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        seedId = runCatching { database.get().mostPlayedSongs(month, limit = 1).first().firstOrNull()?.id }.getOrNull()
    }
    // A finished set plays straight away and the sheet closes.
    LaunchedEffect(state.result) {
        val result = state.result ?: return@LaunchedEffect
        val items = result.orderedIds.mapNotNull { id ->
            result.songsById[id]?.toMediaMetadata() ?: result.seedSong?.takeIf { it.id == id }?.toMediaMetadata()
        }
        if (items.isNotEmpty()) {
            playerConnection?.playQueue(ListQueue(title = result.playlistName, items = items.map { it.toMediaItem() }))
            onDismiss()
        }
    }

    LiquidPortalSheet(onDismiss = onDismiss, title = "Tu sesión de DJ", detents = listOf(0.62f, 0.9f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            androidx.compose.material3.Text(
                "Una mezcla continua a partir de lo que más escuchas.",
                color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp, lineHeight = 20.sp,
            )
            SectionLabel("Ambiente")
            LiquidSegmented(
                items = moods,
                selectedIndex = mood,
                onSelect = { mood = it },
                modifier = Modifier.fillMaxWidth(),
                colors = LiquidSegmentedDefaults.colors(accent = Accent),
            )
            SectionLabel("Duración")
            LiquidSegmented(
                items = durations,
                selectedIndex = duration,
                onSelect = { duration = it },
                modifier = Modifier.fillMaxWidth(),
                colors = LiquidSegmentedDefaults.colors(accent = Accent),
            )
            Spacer(Modifier.height(22.dp))
            GlassPill(
                when {
                    state.isRunning -> "Preparando la sesión…"
                    state.failed -> "No se pudo; reintentar"
                    else -> "Empezar sesión"
                },
                Modifier.fillMaxWidth(), primary = true,
            ) {
                if (state.isRunning) return@GlassPill
                val seconds = longArrayOf(30L * 60, 60L * 60, 120L * 60)[duration]
                viewModel.reset()
                viewModel.generate(
                    prompt = "${moods[mood]} mix",
                    seedSongId = seedId,
                    songCount = 20,
                    targetDurationSeconds = seconds,
                )
            }
            if (state.isRunning) {
                androidx.compose.material3.Text(
                    state.logs.lastOrNull().orEmpty(), color = Muted, fontSize = 12.sp, maxLines = 2,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
