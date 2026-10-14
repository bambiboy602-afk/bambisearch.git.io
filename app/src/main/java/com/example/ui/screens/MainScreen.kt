package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.InvestigationViewModel
import com.example.ui.components.AdvancedFilterDialog
import com.example.ui.components.DossierReportDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class NavTab(val title: String) {
    COCKPIT("Cockpit"),
    EVIDENCE("Evidence"),
    LEADS("Leads Queue")
}

@Composable
fun MainScreen(viewModel: InvestigationViewModel) {
    var selectedTab by remember { mutableStateOf(NavTab.COCKPIT) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDossierDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val progress by viewModel.progress.collectAsState()
    val searchFilters by viewModel.searchFilters.collectAsState()
    val evidenceList by viewModel.evidenceList.collectAsState()
    val leadsList by viewModel.leadsList.collectAsState()
    val searchLogs by viewModel.searchLogs.collectAsState()
    val currentFilter by viewModel.evidenceFilter.collectAsState()

    // Providers state
    var enableArchive by remember { mutableStateOf(true) }
    var enableGithub by remember { mutableStateOf(true) }
    var enableReddit by remember { mutableStateOf(true) }
    var enableWiki by remember { mutableStateOf(true) }
    var enableFirecrawl by remember { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ObsidianBg,
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                contentColor = TextPrimary,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                // Tab 1: Cockpit
                NavigationBarItem(
                    selected = selectedTab == NavTab.COCKPIT,
                    onClick = { selectedTab = NavTab.COCKPIT },
                    icon = {
                        Icon(Icons.Default.Terminal, contentDescription = "Cockpit")
                    },
                    label = {
                        Text(
                            text = "Cockpit",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == NavTab.COCKPIT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navBarColors(),
                    modifier = Modifier.testTag("nav_cockpit")
                )

                // Tab 2: Evidence Dossier
                NavigationBarItem(
                    selected = selectedTab == NavTab.EVIDENCE,
                    onClick = { selectedTab = NavTab.EVIDENCE },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (evidenceList.isNotEmpty()) {
                                    Badge(containerColor = CyberEmerald, contentColor = Color.Black) {
                                        Text(
                                            text = "${evidenceList.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Evidence")
                        }
                    },
                    label = {
                        Text(
                            text = "Evidence",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == NavTab.EVIDENCE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navBarColors(),
                    modifier = Modifier.testTag("nav_evidence")
                )

                // Tab 3: Leads Queue
                NavigationBarItem(
                    selected = selectedTab == NavTab.LEADS,
                    onClick = { selectedTab = NavTab.LEADS },
                    icon = {
                        BadgedBox(
                            badge = {
                                val pending = leadsList.count { it.status == "PENDING" }
                                if (pending > 0) {
                                    Badge(containerColor = CyberCyan, contentColor = Color.Black) {
                                        Text(
                                            text = "$pending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = "Leads")
                        }
                    },
                    label = {
                        Text(
                            text = "Leads",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == NavTab.LEADS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navBarColors(),
                    modifier = Modifier.testTag("nav_leads")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavTab.COCKPIT -> CockpitScreen(
                    progress = progress,
                    searchLogs = searchLogs,
                    searchFilters = searchFilters,
                    onStartSearch = { query, budget ->
                        viewModel.startInvestigation(
                            query = query,
                            budget = budget,
                            filters = searchFilters,
                            enableArchive = enableArchive,
                            enableGithub = enableGithub,
                            enableReddit = enableReddit,
                            enableWikipedia = enableWiki,
                            enableFirecrawl = enableFirecrawl
                        )
                    },
                    onPause = { viewModel.pauseInvestigation() },
                    onResume = { viewModel.resumeInvestigation() },
                    onStop = { viewModel.stopInvestigation() },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenDossier = { showDossierDialog = true },
                    onOpenFilters = { showFilterDialog = true },
                    onApplyQuickStrategy = { strategy, query, dateFilter, fileFilter ->
                        val updated = searchFilters.copy(
                            strategyFocus = strategy,
                            dateRange = dateFilter ?: searchFilters.dateRange,
                            fileType = fileFilter ?: searchFilters.fileType
                        )
                        viewModel.updateSearchFilters(updated)
                    }
                )

                NavTab.EVIDENCE -> EvidenceScreen(
                    evidenceList = evidenceList,
                    currentFilter = currentFilter,
                    onFilterSelect = { viewModel.setEvidenceFilter(it) },
                    onExportDossier = { showDossierDialog = true }
                )

                NavTab.LEADS -> LeadsScreen(
                    leads = leadsList,
                    onAddManualLead = { viewModel.addManualLead(it) }
                )
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current

        // Dialogs
        if (showFilterDialog) {
            AdvancedFilterDialog(
                initialFilters = searchFilters,
                onApply = { newFilters ->
                    viewModel.updateSearchFilters(newFilters)
                },
                onDismiss = { showFilterDialog = false }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                initialGeminiKey = "",
                initialFirecrawlKey = "",
                enableArchive = enableArchive,
                enableGithub = enableGithub,
                enableReddit = enableReddit,
                enableWikipedia = enableWiki,
                enableFirecrawl = enableFirecrawl,
                onSave = { geminiKey, firecrawlKey, arc, git, red, wik, fire ->
                    viewModel.updateApiKeys(geminiKey, firecrawlKey)
                    enableArchive = arc
                    enableGithub = git
                    enableReddit = red
                    enableWiki = wik
                    enableFirecrawl = fire
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showDossierDialog) {
            DossierReportDialog(
                markdownDossier = viewModel.generateMarkdownDossier(),
                jsonDossier = viewModel.generateJsonDossier(),
                onSaveJsonFile = { uri, content ->
                    viewModel.saveDossierToUri(context.contentResolver, uri, content)
                },
                onDismiss = { showDossierDialog = false }
            )
        }
    }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CyberEmerald,
    selectedTextColor = CyberEmerald,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextMuted,
    indicatorColor = SlateSurfaceElevated
)
