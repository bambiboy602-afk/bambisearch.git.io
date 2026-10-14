package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DossierFormat(val label: String) {
    MARKDOWN("Markdown (.md)"),
    JSON("Structured JSON (.json)")
}

@Composable
fun DossierReportDialog(
    markdownDossier: String,
    jsonDossier: String,
    onSaveJsonFile: (Uri, String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var activeFormat by remember { mutableStateOf(DossierFormat.JSON) }

    // System SAF document creator launcher to save .json file locally
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = onSaveJsonFile(uri, jsonDossier)
            if (success) {
                Toast.makeText(context, "Dossier exported to JSON successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save JSON document.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val activeContent = if (activeFormat == DossierFormat.JSON) jsonDossier else markdownDossier

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESEARCH DOSSIER EXPORT",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Format selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFormat == DossierFormat.JSON,
                        onClick = { activeFormat = DossierFormat.JSON },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DataObject,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("JSON (.json)", fontSize = 11.sp) },
                        colors = formatChipColors(activeFormat == DossierFormat.JSON),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFormat == DossierFormat.JSON,
                            borderColor = SlateSurfaceBorder
                        ),
                        modifier = Modifier.testTag("format_json_chip")
                    )

                    FilterChip(
                        selected = activeFormat == DossierFormat.MARKDOWN,
                        onClick = { activeFormat = DossierFormat.MARKDOWN },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("Markdown (.md)", fontSize = 11.sp) },
                        colors = formatChipColors(activeFormat == DossierFormat.MARKDOWN),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFormat == DossierFormat.MARKDOWN,
                            borderColor = SlateSurfaceBorder
                        ),
                        modifier = Modifier.testTag("format_md_chip")
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp)
            ) {
                // Scrollable preview box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurfaceElevated)
                        .border(1.dp, SlateSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = activeContent,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary File Save Button (JSON Export)
                Button(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        createJsonLauncher.launch("leadtrace_dossier_$timestamp.json")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_json_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save as Local JSON File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Copy & Share buttons
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(activeContent))
                            val formatName = if (activeFormat == DossierFormat.JSON) "JSON" else "Markdown"
                            Toast.makeText(context, "$formatName dossier copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan.copy(alpha = 0.2f),
                            contentColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_dossier_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, activeContent)
                                type = if (activeFormat == DossierFormat.JSON) "application/json" else "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Research Findings"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateSurfaceBorder,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_dossier_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceBorder)
            ) {
                Text("Close", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun formatChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = SlateSurfaceElevated,
    labelColor = if (selected) CyberEmerald else TextSecondary,
    selectedContainerColor = CyberEmerald.copy(alpha = 0.2f),
    selectedLabelColor = CyberEmerald,
    selectedLeadingIconColor = CyberEmerald
)
