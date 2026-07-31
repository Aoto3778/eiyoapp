package jp.aoto.eiyoapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.aoto.eiyoapp.domain.Exporter

@Composable
fun PageHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom=12.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
        Text(title, style=androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        if (action != null && onAction != null) TextButtonLike(action, onAction)
    }
    HorizontalDivider(color=Rule)
}

@Composable
fun TextButtonLike(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    androidx.compose.material3.TextButton(onClick=onClick, enabled=enabled, colors=ButtonDefaults.textButtonColors(contentColor=Accent)) { Text(text) }
}

@Composable
fun LineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick=onClick, modifier=modifier, enabled=enabled, shape=androidx.compose.ui.graphics.RectangleShape,
        border=BorderStroke(1.dp, if (enabled) Accent else Rule),
        colors=ButtonDefaults.outlinedButtonColors(contentColor=Ink),
    ) { Text(text) }
}

@Composable
fun PaperField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String? = null, placeholder: String? = null, singleLine: Boolean = true,
) {
    OutlinedTextField(
        value=value, onValueChange=onValueChange, modifier=modifier, singleLine=singleLine,
        label=label?.let { { Text(it) } }, placeholder=placeholder?.let { { Text(it) } },
        shape=androidx.compose.ui.graphics.RectangleShape,
        colors=OutlinedTextFieldDefaults.colors(
            focusedBorderColor=Accent, unfocusedBorderColor=Rule, focusedContainerColor=Color.Transparent,
            unfocusedContainerColor=Color.Transparent,
        ),
    )
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value, style=androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text(label, color=Muted, style=androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProgressLine(progress: Float) {
    Box(Modifier.fillMaxWidth().height(3.dp).clipToBounds()) {
        Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.Center).then(Modifier), contentAlignment=Alignment.Center) { HorizontalDivider(color=Rule) }
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).align(Alignment.CenterStart), contentAlignment=Alignment.Center) { HorizontalDivider(thickness=3.dp, color=Accent) }
    }
}

fun number(value: Double) = Exporter.fmt(value)
