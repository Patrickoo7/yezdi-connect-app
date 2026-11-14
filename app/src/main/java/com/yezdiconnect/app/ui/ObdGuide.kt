package com.yezdiconnect.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yezdiconnect.app.R

/** "How it works" link shown next to OBD features. */
@Composable
fun HowItWorksLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(stringResource(R.string.guide_link), color = YezdiColors.Accent, style = MaterialTheme.typography.labelLarge)
    }
}

/** Bottom sheet explaining OBD-2 and bike health in plain language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObdGuideSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = YezdiColors.Background,
        contentColor = YezdiColors.Text,
    ) {
        ObdGuideContent(Modifier.verticalScroll(rememberScrollState()))
    }
}

@Composable
fun ObdGuideContent(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.guide_title), stringResource(R.string.guide_subtitle))

        SectionLabel(stringResource(R.string.guide_what_label))
        SurfaceCard {
            Text(stringResource(R.string.guide_what_1), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.guide_what_2), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.guide_what_3), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, modifier = Modifier.padding(top = 8.dp))
        }

        SectionLabel(stringResource(R.string.guide_how_label))
        ConnectionDiagram()

        SectionLabel(stringResource(R.string.guide_need_label))
        SurfaceCard {
            Text(stringResource(R.string.guide_need_1), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.guide_need_2), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, modifier = Modifier.padding(top = 8.dp))
        }
        NoticeBox(stringResource(R.string.guide_need_photo))

        SectionLabel(stringResource(R.string.guide_steps_label))
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberedStep(1, stringResource(R.string.guide_step_1))
                NumberedStep(2, stringResource(R.string.guide_step_2))
                NumberedStep(3, stringResource(R.string.guide_step_3))
                NumberedStep(4, stringResource(R.string.guide_step_4))
            }
        }

        SectionLabel(stringResource(R.string.guide_results_label))
        SurfaceCard {
            Text(stringResource(R.string.health_ok), style = MaterialTheme.typography.titleMedium, color = YezdiColors.Ok)
            Text(stringResource(R.string.guide_result_ok), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
            Text("P0301", style = MaterialTheme.typography.titleMedium, color = YezdiColors.Error, modifier = Modifier.padding(top = 12.dp))
            Text(stringResource(R.string.guide_result_code), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
            Text(stringResource(R.string.health_pending), style = MaterialTheme.typography.titleMedium, color = YezdiColors.Warn, modifier = Modifier.padding(top = 12.dp))
            Text(stringResource(R.string.guide_result_pending), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
        }

        SectionLabel(stringResource(R.string.guide_warning_label))
        SurfaceCard {
            Text(stringResource(R.string.guide_warning_battery), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.guide_warning_temp), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.guide_warning_note), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Bike computer → socket → adapter → phone, drawn with simple boxes. */
@Composable
private fun ConnectionDiagram() {
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            DiagramBox(stringResource(R.string.guide_box_bike), stringResource(R.string.guide_box_bike_sub), Modifier.weight(1f))
            Arrow(stringResource(R.string.guide_arrow_plug))
            DiagramBox(stringResource(R.string.guide_box_adapter), stringResource(R.string.guide_box_adapter_sub), Modifier.weight(1f), highlight = true)
            Arrow(stringResource(R.string.guide_arrow_bt))
            DiagramBox(stringResource(R.string.guide_box_phone), stringResource(R.string.guide_box_phone_sub), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DiagramBox(title: String, sub: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val color = if (highlight) YezdiColors.Accent else YezdiColors.Outline
    Column(
        modifier
            .background(YezdiColors.SurfaceHigh, MaterialTheme.shapes.small)
            .border(BorderStroke(1.dp, color), MaterialTheme.shapes.small)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = if (highlight) YezdiColors.Accent else YezdiColors.Text)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = YezdiColors.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Arrow(label: String) {
    Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("→", color = YezdiColors.Accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = YezdiColors.Muted)
        }
    }
}
