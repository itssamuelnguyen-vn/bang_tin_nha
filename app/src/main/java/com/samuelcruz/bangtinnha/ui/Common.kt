package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samuelcruz.bangtinnha.core.db.SourceStatus
import com.samuelcruz.bangtinnha.core.formatAge
import kotlin.time.Duration

/** A dashboard card: title row, optional status line, then the module's content. Tapping opens the detail. */
@Composable
fun ModuleCard(
    emoji: String,
    title: String,
    status: SourceStatus?,
    interval: Duration,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            StatusLine(status, interval)
            Spacer(Modifier.padding(top = 8.dp))
            content()
        }
    }
}

/**
 * How fresh the data is: an error (shown on this card only), "Chưa cập nhật X" once older than twice the interval,
 * otherwise a quiet "Cập nhật X trước".
 */
@Composable
fun StatusLine(status: SourceStatus?, interval: Duration, now: Long = System.currentTimeMillis()) {
    val last = status?.lastSuccessAt
    val (text, color) = when {
        status?.lastError != null ->
            "⚠ Lỗi: ${status.lastError}" + (last?.let { " · dữ liệu từ ${formatAge(it, now)} trước" } ?: "") to
                MaterialTheme.colorScheme.error
        last == null -> "Chưa có dữ liệu: kéo xuống để tải" to MaterialTheme.colorScheme.onSurfaceVariant
        now - last > 2 * interval.inWholeMilliseconds -> "Chưa cập nhật ${formatAge(last, now)}" to Color(0xFFE65100)
        else -> "Cập nhật ${formatAge(last, now)} trước" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 2)
}

/** Scaffold with a back arrow, for every screen but home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
                },
            )
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
