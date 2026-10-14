package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    initialGeminiKey: String,
    initialFirecrawlKey: String,
    enableArchive: Boolean,
    enableGithub: Boolean,
    enableReddit: Boolean,
    enableWikipedia: Boolean,
    enableFirecrawl: Boolean,
    onSave: (
        geminiKey: String,
        firecrawlKey: String,
        archive: Boolean,
        github: Boolean,
        reddit: Boolean,
        wiki: Boolean,
        firecrawl: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var geminiKey by remember { mutableStateOf(initialGeminiKey) }
    var firecrawlKey by remember { mutableStateOf(initialFirecrawlKey) }

    var archiveState by remember { mutableStateOf(enableArchive) }
    var githubState by remember { mutableStateOf(enableGithub) }
    var redditState by remember { mutableStateOf(enableReddit) }
    var wikiState by remember { mutableStateOf(enableWikipedia) }
    var firecrawlState by remember { mutableStateOf(enableFirecrawl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        title = {
            Text(
                text = "ENGINE & SEARCH SETTINGS",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Specialist Search Sources",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                SourceCheckbox(label = "Internet Archive & Wayback Machine", checked = archiveState) { archiveState = it }
                SourceCheckbox(label = "GitHub Repositories & Docs", checked = githubState) { githubState = it }
                SourceCheckbox(label = "Reddit Discussions & Forums", checked = redditState) { redditState = it }
                SourceCheckbox(label = "Wikimedia / Open Records", checked = wikiState) { wikiState = it }
                SourceCheckbox(label = "Firecrawl & Web Extraction", checked = firecrawlState) { firecrawlState = it }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Optional API Credentials",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Configured via Secrets panel or enter directly here:",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini API Key", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberEmerald,
                        unfocusedBorderColor = SlateSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("gemini_key_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = firecrawlKey,
                    onValueChange = { firecrawlKey = it },
                    label = { Text("Firecrawl API Key (Optional)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("firecrawl_key_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(geminiKey, firecrawlKey, archiveState, githubState, redditState, wikiState, firecrawlState)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                modifier = Modifier.testTag("save_settings_btn")
            ) {
                Text("Save Configuration", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun SourceCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CyberEmerald, checkmarkColor = Color.Black)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}
