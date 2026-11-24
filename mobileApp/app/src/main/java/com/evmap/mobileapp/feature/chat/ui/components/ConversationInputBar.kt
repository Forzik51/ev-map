package feature.chat.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import core.designsystem.Spacing

@Composable
fun ConversationInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onStartRecord: () -> Unit,
    enabled: Boolean,
    canSendText: Boolean,
) {
    val sendText = enabled && canSendText && text.isNotBlank()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            placeholder = { Text("Message") },
            shape = RoundedCornerShape(28.dp),
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = if (canSendText) ImeAction.Send else ImeAction.Default),
            keyboardActions = KeyboardActions(onSend = { if (sendText) onSend() }),
            leadingIcon = {
                IconButton(onClick = onAttach, enabled = enabled) {
                    Icon(Icons.Default.Add, contentDescription = "Attach file")
                }
            },
            trailingIcon = {
                if (canSendText && text.isNotBlank()) {
                    IconButton(onClick = onSend, enabled = sendText) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                } else if (canSendText) {
                    IconButton(onClick = onStartRecord, enabled = enabled) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Record voice message")
                    }
                }
            },
        )
    }
}
