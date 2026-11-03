package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samuelcruz.bangtinnha.core.db.SourceStatus
import com.samuelcruz.bangtinnha.core.formatAge
import kotlin.time.Duration

/** Emoji on a gradient circle: the coloured "app icon" of each module. */
@Composable
fun IconBubble(emoji: String, colors: ModuleColors, size: Int = 44) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(colors.gradient), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size * 0.5).sp)
    }
}

/** A small rounded label. */
@Composable
fun Pill(text: String, background: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Freshness of a source: an error (on this card only), "Chưa cập nhật X" after 2 × interval, else "X trước". */
sealed interface Freshness {
    data class Fresh(val text: String) : Freshness
    data class Stale(val text: String) : Freshness
    data class Failed(val message: String, val dataAge: String?) : Freshness
    data object Empty : Freshness
}

fun freshness(status: SourceStatus?, interval: Duration, now: Long = System.currentTimeMillis()): Freshness {
    val last = status?.lastSuccessAt
    return when {
        status?.lastError != null -> Freshness.Failed(status.lastError, last?.let { formatAge(it, now) })
        last == null -> Freshness.Empty
        now - last > 2 * interval.inWholeMilliseconds -> Freshness.Stale("Chưa cập nhật ${formatAge(last, now)}")
        else -> Freshness.Fresh(formatAge(last, now).let { if (it == "vừa xong") it else "$it trước" })
    }
}

/** A dashboard card: coloured icon, title and freshness, then the module's content. Tapping opens the details. */
@Composable
fun ModuleCard(
    emoji: String,
    title: String,
    colors: ModuleColors,
    status: SourceStatus?,
    interval: Duration,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val f = freshness(status, interval)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(emoji, colors)
                Spacer(Modifier.size(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when (f) {
                    is Freshness.Fresh -> Pill(f.text, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                    is Freshness.Stale -> Pill(f.text, Palette.warn.copy(alpha = 0.18f), Palette.warn)
                    is Freshness.Failed -> Pill("Lỗi", MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
                    Freshness.Empty -> Pill("Chưa có", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (f is Freshness.Failed) ErrorBanner(f)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ErrorBanner(f: Freshness.Failed) {
    Text(
        "⚠ ${f.message}" + (f.dataAge?.let { "\nĐang hiện dữ liệu từ $it trước" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        maxLines = 3,
        modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f)).padding(10.dp),
    )
}

/**
 * Every screen but home: a gradient header with back arrow, title and optional subtitle, then the content on a
 * rounded sheet that overlaps the header.
 */
@Composable
fun SubScreen(
    title: String,
    onBack: () -> Unit,
    colors: ModuleColors = Palette.settings,
    subtitle: String? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().background(colors.gradient).windowInsetsPadding(WindowInsets.statusBars)) {
            Column(Modifier.padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 40.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp),
                )
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize().offset(y = (-24).dp),
        ) {
            content(Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

/** A rounded tile for lists inside detail screens. */
@Composable
fun Tile(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() } }
}
