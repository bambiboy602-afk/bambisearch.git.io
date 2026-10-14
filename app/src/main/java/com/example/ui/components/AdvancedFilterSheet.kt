package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.DateRangeFilter
import com.example.data.model.DomainRestrictionFilter
import com.example.data.model.FileTypeFilter
import com.example.data.model.SearchFilters
import com.example.data.model.StrategyFocus
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdvancedFilterDialog(
    initialFilters: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var strategy by remember { mutableStateOf(initialFilters.strategyFocus) }
    var dateRange by remember { mutableStateOf(initialFilters.dateRange) }
    var fileType by remember { mutableStateOf(initialFilters.fileType) }
    var domainRestriction by remember { mutableStateOf(initialFilters.domainRestriction) }
    var customDomain by remember { mutableStateOf(initialFilters.customDomain) }
    var customStartYear by remember { mutableStateOf(initialFilters.customStartYear?.toString() ?: "") }
    var customEndYear by remember { mutableStateOf(initialFilters.customEndYear?.toString() ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "ADVANCED SEARCH FILTERS",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(scrollState)
            ) {
                // Section 1: Research Strategy & Topical Focus
                Text(
                    text = "1. STRATEGY FOCUS & TOPIC SCOPE",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Steers the agent's alternate query generation and terminology extraction:",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                StrategyFocus.values().forEach { item ->
                    FilterChip(
                        selected = strategy == item,
                        onClick = { strategy = item },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(item.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(item.description, fontSize = 10.sp, color = TextMuted)
                            }
                        },
                        colors = filterChipColors(strategy == item),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = strategy == item,
                            borderColor = SlateSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("strategy_chip_${item.name.lowercase()}")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Date Range
                Text(
                    text = "2. DATE RANGE RESTRICTION",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Target historical centuries or modern post-2020 releases:",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateRangeFilter.values().forEach { range ->
                        FilterChip(
                            selected = dateRange == range,
                            onClick = { dateRange = range },
                            label = { Text(range.label, fontSize = 11.sp) },
                            colors = filterChipColors(dateRange == range),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = dateRange == range,
                                borderColor = SlateSurfaceBorder
                            ),
                            modifier = Modifier.testTag("date_chip_${range.name.lowercase()}")
                        )
                    }
                }

                if (dateRange == DateRangeFilter.CUSTOM) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customStartYear,
                            onValueChange = { customStartYear = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = { Text("From Year", fontSize = 11.sp) },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.weight(1f).testTag("custom_start_year")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customEndYear,
                            onValueChange = { customEndYear = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = { Text("To Year", fontSize = 11.sp) },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.weight(1f).testTag("custom_end_year")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: File Type Filter
                Text(
                    text = "3. FILE TYPE TARGETING",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Filter searches for specific file extensions and raw formats:",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FileTypeFilter.values().forEach { type ->
                        FilterChip(
                            selected = fileType == type,
                            onClick = { fileType = type },
                            label = { Text(type.label, fontSize = 11.sp) },
                            colors = filterChipColors(fileType == type),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = fileType == type,
                                borderColor = SlateSurfaceBorder
                            ),
                            modifier = Modifier.testTag("filetype_chip_${type.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: Domain Restrictions
                Text(
                    text = "4. DOMAIN RESTRICTIONS",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Restrict sources to official government, academic, or custom domains:",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DomainRestrictionFilter.values().forEach { dom ->
                        FilterChip(
                            selected = domainRestriction == dom,
                            onClick = { domainRestriction = dom },
                            label = { Text(dom.label, fontSize = 11.sp) },
                            colors = filterChipColors(domainRestriction == dom),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = domainRestriction == dom,
                                borderColor = SlateSurfaceBorder
                            ),
                            modifier = Modifier.testTag("domain_chip_${dom.name.lowercase()}")
                        )
                    }
                }

                if (domainRestriction == DomainRestrictionFilter.CUSTOM) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customDomain,
                        onValueChange = { customDomain = it.trim() },
                        placeholder = { Text("e.g., nih.gov or archive.org", fontSize = 12.sp, color = TextMuted) },
                        label = { Text("Custom Domain (site:)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = textFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("custom_domain_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sYear = customStartYear.toIntOrNull()
                    val eYear = customEndYear.toIntOrNull()
                    val result = SearchFilters(
                        dateRange = dateRange,
                        customStartYear = sYear,
                        customEndYear = eYear,
                        fileType = fileType,
                        domainRestriction = domainRestriction,
                        customDomain = customDomain,
                        strategyFocus = strategy
                    )
                    onApply(result)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                modifier = Modifier.testTag("apply_filters_btn")
            ) {
                Text("Apply Parameters", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Reset to defaults
                    strategy = StrategyFocus.GENERAL
                    dateRange = DateRangeFilter.ALL_TIME
                    fileType = FileTypeFilter.ALL
                    domainRestriction = DomainRestrictionFilter.ALL
                    customDomain = ""
                    customStartYear = ""
                    customEndYear = ""
                },
                modifier = Modifier.testTag("reset_filters_btn")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Reset Defaults", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun filterChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = SlateSurfaceElevated,
    labelColor = if (selected) CyberEmerald else TextSecondary,
    selectedContainerColor = CyberEmerald.copy(alpha = 0.2f),
    selectedLabelColor = CyberEmerald
)

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = CyberEmerald,
    unfocusedBorderColor = SlateSurfaceBorder,
    focusedContainerColor = SlateSurfaceElevated,
    unfocusedContainerColor = SlateSurfaceElevated
)
