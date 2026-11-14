package com.yezdiconnect.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Two-tone, wide-spaced app name: "YEZDI" in text color, "CONNECT" in accent. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append("YEZDI")
            withStyle(SpanStyle(color = YezdiColors.Accent)) { append("CONNECT") }
        },
        modifier = modifier,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.3.em,
        color = YezdiColors.Text,
    )
}

/** Big uppercase screen title with a muted subtitle. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = YezdiColors.Muted)
        }
    }
}

/** Small, uppercase, wide-spaced label above a group of cards. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = YezdiColors.Muted,
    )
}

/** Surface card with the thin outline used throughout the app. */
@Composable
fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = YezdiColors.Surface, contentColor = YezdiColors.Text),
        border = BorderStroke(1.dp, YezdiColors.Outline),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

/** A big number with a small label, used in two-column grids. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = YezdiColors.Text) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = YezdiColors.Surface, contentColor = YezdiColors.Text),
        border = BorderStroke(1.dp, YezdiColors.Outline),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, maxLines = 1)
        }
    }
}

/** Two equal tiles side by side. */
@Composable
fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Full-width accent button. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = YezdiColors.Accent,
            contentColor = Color.White,
            disabledContainerColor = YezdiColors.Accent.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.6f),
        ),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

enum class BannerKind { Ok, Warn, Error }

/** Full-width status strip with a colored dot, shown under the top bar. */
@Composable
fun StatusBanner(text: String, kind: BannerKind, modifier: Modifier = Modifier) {
    val (fg, bg) = when (kind) {
        BannerKind.Ok -> YezdiColors.Ok to YezdiColors.Ok.copy(alpha = 0.14f)
        BannerKind.Warn -> YezdiColors.Warn to YezdiColors.WarnBg
        BannerKind.Error -> YezdiColors.Error to YezdiColors.ErrorBg
    }
    Row(
        modifier.fillMaxWidth().background(bg).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(fg, CircleShape))
        Spacer(Modifier.size(10.dp))
        Text(text, color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

/** Yellow notice box, e.g. for a missing setting. */
@Composable
fun NoticeBox(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .background(YezdiColors.WarnBg, MaterialTheme.shapes.small)
            .padding(14.dp),
        color = YezdiColors.Warn,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** Numbered step: accent-tinted circle with the number, text beside it. */
@Composable
fun NumberedStep(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(28.dp).background(YezdiColors.Accent.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$number", color = YezdiColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}
