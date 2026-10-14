package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DateRangeFilter
import com.example.data.model.FileTypeFilter
import com.example.data.model.InvestigationStatus
import com.example.data.model.SearchFilters
import com.example.data.model.SearchLogEntry
import com.example.data.model.StrategyFocus
import com.example.domain.EngineProgress
import com.example.ui.components.LogItemView
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CockpitScreen(
    progress: EngineProgress,
    searchLogs: List<SearchLogEntry>,
    searchFilters: SearchFilters,
    onStartSearch: (query: String, budget: Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDossier: () -> Unit,
    onOpenFilters: () -> Unit,
    onApplyQuickStrategy: (StrategyFocus, String, DateRangeFilter?, FileTypeFilter?) -> Unit
) {
    var queryInput by remember { mutableStateOf("") }
    var budgetValue by remember { mutableFloatStateOf(20f) }

    val logListState = rememberLazyListState()

    // Auto-scroll to latest log
    LaunchedEffect(searchLogs.size) {
        if (searchLogs.isNotEmpty()) {
            logListState.animateScrollToItem(searchLogs.size - 1)
        }
    }

    // Pulse animation for radar indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(if (progress.status == InvestigationStatus.RUNNING) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            when (progress.status) {
                                InvestigationStatus.RUNNING -> CyberEmerald
                                InvestigationStatus.PAUSED -> CyberAmber
                                InvestigationStatus.COMPLETED -> CyberCyan
                                else -> TextMuted
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LEADTRACE",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "DEEPSEARCH RESEARCH BOT",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row {
                // Filters button with active indicator badge
                IconButton(
                    onClick = onOpenFilters,
                    modifier = Modifier.testTag("filter_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (searchFilters.isActive()) {
                                Badge(containerColor = CyberEmerald, contentColor = Color.Black) {
                                    Text("✓", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Search Parameters",
                            tint = if (searchFilters.isActive()) CyberEmerald else TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenDossier,
                    modifier = Modifier.testTag("dossier_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "View Dossier",
                        tint = CyberEmerald
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Parameters Indicator Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SlateSurfaceElevated)
                .border(1.dp, SlateSurfaceBorder, RoundedCornerShape(6.dp))
                .clickable { onOpenFilters() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("active_filters_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                    tint = if (searchFilters.isActive()) CyberEmerald else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scope: ${searchFilters.toDisplaySummary()}",
                    color = if (searchFilters.isActive()) CyberEmerald else TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            Text(
                text = "Adjust",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inquiry Input
        OutlinedTextField(
            value = queryInput,
            onValueChange = { queryInput = it },
            placeholder = {
                Text(
                    text = "Research topic, book title, manual, or obscure file...",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CyberEmerald,
                unfocusedBorderColor = SlateSurfaceBorder,
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (queryInput.isNotBlank()) {
                            onStartSearch(queryInput.trim(), budgetValue.toInt())
                        }
                    },
                    modifier = Modifier.testTag("launch_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (queryInput.isNotBlank()) CyberEmerald else TextMuted
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_query_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Strategy & Refinement Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. 19th Century Historical Documents preset
            SuggestionChip(
                onClick = {
                    queryInput = "historical documents from the 19th century letters manuscripts"
                    onApplyQuickStrategy(
                        StrategyFocus.HISTORICAL_19TH,
                        queryInput,
                        DateRangeFilter.NINETEENTH_CENTURY,
                        FileTypeFilter.PDF_ONLY
                    )
                },
                label = { Text("19th Century History", fontSize = 11.sp, color = TextSecondary) },
                colors = SuggestChipColors(),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = SlateSurfaceBorder
                ),
                modifier = Modifier.testTag("preset_19th_century")
            )

            // 2. Climate Change Datasets preset
            SuggestionChip(
                onClick = {
                    queryInput = "public datasets related to climate change global temperature"
                    onApplyQuickStrategy(
                        StrategyFocus.CLIMATE_DATASETS,
                        queryInput,
                        DateRangeFilter.AFTER_2020,
                        FileTypeFilter.DATASETS
                    )
                },
                label = { Text("Climate Datasets", fontSize = 11.sp, color = TextSecondary) },
                colors = SuggestChipColors(),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = SlateSurfaceBorder
                ),
                modifier = Modifier.testTag("preset_climate_data")
            )

            // 3. Technical Manuals & Specs
            SuggestionChip(
                onClick = {
                    queryInput = "technical manual specification documentation guide"
                    onApplyQuickStrategy(
                        StrategyFocus.TECHNICAL_LABS,
                        queryInput,
                        DateRangeFilter.ALL_TIME,
                        FileTypeFilter.DOCUMENTS
                    )
                },
                label = { Text("Manuals & Specs", fontSize = 11.sp, color = TextSecondary) },
                colors = SuggestChipColors(),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = SlateSurfaceBorder
                ),
                modifier = Modifier.testTag("preset_manuals")
            )

            // 4. Civic & Declassified Records
            SuggestionChip(
                onClick = {
                    queryInput = "declassified official reports government transcripts"
                    onApplyQuickStrategy(
                        StrategyFocus.LEGAL_DECLASSIFIED,
                        queryInput,
                        DateRangeFilter.ALL_TIME,
                        FileTypeFilter.PDF_ONLY
                    )
                },
                label = { Text("Declassified & Civic", fontSize = 11.sp, color = TextSecondary) },
                colors = SuggestChipColors(),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = SlateSurfaceBorder
                ),
                modifier = Modifier.testTag("preset_declassified")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Budget & Control Cockpit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateSurfaceBorder)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUERY BUDGET GUARDRAIL",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${progress.queriesUsed} / ${progress.maxBudget} queries",
                        color = if (progress.queriesUsed >= progress.maxBudget) CyberRed else CyberEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progressFrac = if (progress.maxBudget > 0) {
                    (progress.queriesUsed.toFloat() / progress.maxBudget.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progressFrac },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progressFrac > 0.8f) CyberAmber else CyberEmerald,
                    trackColor = SlateSurfaceElevated
                )

                // Budget slider if not running
                if (progress.status == InvestigationStatus.IDLE || progress.status == InvestigationStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Set Cap: ${budgetValue.toInt()} queries",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = budgetValue,
                            onValueChange = { budgetValue = it },
                            valueRange = 5f..50f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = SlateSurfaceElevated
                            ),
                            modifier = Modifier
                                .width(180.dp)
                                .testTag("budget_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status message & Provider tag
                Text(
                    text = progress.statusMessage,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cockpit Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (progress.status == InvestigationStatus.RUNNING) {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("pause_btn")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (progress.status == InvestigationStatus.PAUSED) {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("resume_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (progress.status == InvestigationStatus.RUNNING || progress.status == InvestigationStatus.PAUSED) {
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("stop_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = CyberRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Finish Early", color = CyberRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Terminal Log Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INVESTIGATION AUDIT LOG",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${searchLogs.size} events",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Terminal Log Console Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SlateSurfaceElevated)
                .border(1.dp, SlateSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            if (searchLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Terminal ready. Enter research inquiry above.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchLogs) { log ->
                        LogItemView(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestChipColors() = SuggestionChipDefaults.suggestionChipColors(
    containerColor = SlateSurfaceElevated,
    labelColor = TextSecondary
)
