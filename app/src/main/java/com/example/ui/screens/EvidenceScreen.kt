package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvidenceItem
import com.example.data.model.RelevanceClassification
import com.example.ui.components.EvidenceCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurfaceBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EvidenceScreen(
    evidenceList: List<EvidenceItem>,
    currentFilter: RelevanceClassification?,
    onFilterSelect: (RelevanceClassification?) -> Unit,
    onExportDossier: () -> Unit = {}
) {
    val filteredEvidence = if (currentFilter == null) {
        evidenceList
    } else {
        evidenceList.filter { it.classification == currentFilter }
    }

    val instructionalCount = evidenceList.count { it.classification == RelevanceClassification.INSTRUCTIONAL }
    val primaryCount = evidenceList.count { it.classification == RelevanceClassification.PRIMARY_SOURCE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RANKED EVIDENCE DOSSIER",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${evidenceList.size} Total Discovered Sources ($instructionalCount Instructional, $primaryCount Primary)",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            androidx.compose.material3.IconButton(
                onClick = onExportDossier,
                modifier = Modifier.testTag("evidence_export_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Export Findings",
                    tint = CyberEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentFilter == null,
                onClick = { onFilterSelect(null) },
                label = { Text("All (${evidenceList.size})", fontSize = 11.sp) },
                colors = filterColors(currentFilter == null),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = currentFilter == null,
                    borderColor = SlateSurfaceBorder
                ),
                modifier = Modifier.testTag("filter_all")
            )

            listOf(
                RelevanceClassification.INSTRUCTIONAL,
                RelevanceClassification.PRIMARY_SOURCE,
                RelevanceClassification.HISTORICAL,
                RelevanceClassification.REFERENCE,
                RelevanceClassification.NEWS
            ).forEach { classification ->
                val count = evidenceList.count { it.classification == classification }
                FilterChip(
                    selected = currentFilter == classification,
                    onClick = {
                        onFilterSelect(if (currentFilter == classification) null else classification)
                    },
                    label = { Text("${classification.label} ($count)", fontSize = 11.sp) },
                    colors = filterColors(currentFilter == classification),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == classification,
                        borderColor = SlateSurfaceBorder
                    ),
                    modifier = Modifier.testTag("filter_${classification.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content
        if (filteredEvidence.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (evidenceList.isEmpty()) "No evidence collected yet." else "No sources matching this filter.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Launch an investigation in the Cockpit to extract documents.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEvidence) { item ->
                    EvidenceCard(evidence = item)
                }
            }
        }
    }
}

@Composable
private fun filterColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = SlateSurfaceElevated,
    labelColor = if (selected) CyberEmerald else TextSecondary,
    selectedContainerColor = CyberEmerald.copy(alpha = 0.2f),
    selectedLabelColor = CyberEmerald
)
