package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.EvidenceItem
import com.example.data.model.Investigation
import com.example.data.model.RelevanceClassification
import com.example.data.model.SearchLead
import com.example.data.model.SearchLogEntry
import com.example.domain.EngineProgress
import com.example.domain.InvestigationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.data.model.SearchFilters
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InvestigationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val engine = InvestigationEngine(database)

    val progress: StateFlow<EngineProgress> = engine.progress

    // Active Search Filters
    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    // Selected investigation ID for detail view
    private val _selectedInvestigationId = MutableStateFlow<String?>(null)
    val selectedInvestigationId: StateFlow<String?> = _selectedInvestigationId.asStateFlow()

    // All past investigations
    val investigations: StateFlow<List<Investigation>> = database.investigationDao()
        .getAllInvestigations()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Evidence for selected investigation
    val evidenceList: StateFlow<List<EvidenceItem>> = _selectedInvestigationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else database.evidenceDao().getEvidenceForInvestigation(id).map { list -> list.map { it.toDomain() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Leads for selected investigation
    val leadsList: StateFlow<List<SearchLead>> = _selectedInvestigationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else database.leadDao().getLeadsForInvestigation(id).map { list -> list.map { it.toDomain() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logs for selected investigation (Live Activity Feed)
    val searchLogs: StateFlow<List<SearchLogEntry>> = _selectedInvestigationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else database.searchLogDao().getLogsForInvestigation(id).map { list -> list.map { it.toDomain() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected evidence filter
    private val _evidenceFilter = MutableStateFlow<RelevanceClassification?>(null)
    val evidenceFilter: StateFlow<RelevanceClassification?> = _evidenceFilter.asStateFlow()

    fun updateSearchFilters(filters: SearchFilters) {
        _searchFilters.value = filters
    }

    fun setEvidenceFilter(filter: RelevanceClassification?) {
        _evidenceFilter.value = filter
    }

    fun selectInvestigation(id: String) {
        _selectedInvestigationId.value = id
    }

    fun startInvestigation(
        query: String,
        budget: Int = 20,
        filters: SearchFilters = _searchFilters.value,
        enableArchive: Boolean = true,
        enableGithub: Boolean = true,
        enableReddit: Boolean = true,
        enableWikipedia: Boolean = true,
        enableFirecrawl: Boolean = true
    ) {
        val newId = engine.startInvestigation(
            query = query,
            budget = budget,
            filters = filters,
            enableArchive = enableArchive,
            enableGithub = enableGithub,
            enableReddit = enableReddit,
            enableWikipedia = enableWikipedia,
            enableFirecrawl = enableFirecrawl
        )
        _selectedInvestigationId.value = newId
    }

    fun pauseInvestigation() {
        engine.pauseInvestigation()
    }

    fun resumeInvestigation() {
        engine.resumeInvestigation()
    }

    fun stopInvestigation() {
        engine.stopInvestigation()
    }

    fun addManualLead(queryPhrase: String) {
        val currentId = _selectedInvestigationId.value ?: return
        engine.addManualLead(currentId, queryPhrase)
    }

    fun deleteInvestigation(id: String) {
        viewModelScope.launch {
            database.investigationDao().deleteById(id)
            if (_selectedInvestigationId.value == id) {
                _selectedInvestigationId.value = null
            }
        }
    }

    fun updateApiKeys(geminiKey: String, firecrawlKey: String) {
        engine.researchAgent.updateApiKey(geminiKey)
        engine.firecrawlProvider.updateApiKey(firecrawlKey)
    }

    fun generateMarkdownDossier(): String {
        val evidence = evidenceList.value
        val leads = leadsList.value
        val currentId = _selectedInvestigationId.value ?: "N/A"
        val prog = progress.value

        val sb = StringBuilder()
        sb.append("# DEEPSEARCH RESEARCH DOSSIER\n\n")
        sb.append("**Investigation ID**: `$currentId`\n")
        sb.append("**Search Objective**: ${prog.currentQuery}\n")
        sb.append("**Queries Budget**: ${prog.queriesUsed} / ${prog.maxBudget}\n")
        sb.append("**Total Evidence Items Discovered**: ${evidence.size}\n\n")
        sb.append("---\n\n")

        sb.append("## 1. Verified Primary & Instructional Sources\n\n")
        val primaryItems = evidence.filter {
            it.classification == RelevanceClassification.INSTRUCTIONAL ||
                    it.classification == RelevanceClassification.PRIMARY_SOURCE
        }

        if (primaryItems.isEmpty()) {
            sb.append("*No primary instructional manuals or specifications verified yet.*\n\n")
        } else {
            primaryItems.forEachIndexed { idx, item ->
                sb.append("### ${idx + 1}. ${item.title}\n")
                sb.append("- **Direct Link**: [${item.directUrl}](${item.directUrl})\n")
                sb.append("- **Source Provider**: ${item.sourceProvider} | **Date**: ${item.sourceDate}\n")
                sb.append("- **Classification**: ${item.classification.label} (Confidence: ${item.confidenceScore}%)\n")
                sb.append("- **Verification**: ${item.verificationStatus.label}\n")
                sb.append("- **Discovery Query**: `${item.discoveryQuery}`\n")
                sb.append("- **Key Excerpt**:\n> \"${item.verbatimExcerpt}\"\n\n")
            }
        }

        sb.append("## 2. Reference & Historical Archives\n\n")
        val secondaryItems = evidence.filter {
            it.classification != RelevanceClassification.INSTRUCTIONAL &&
                    it.classification != RelevanceClassification.PRIMARY_SOURCE
        }

        if (secondaryItems.isEmpty()) {
            sb.append("*No secondary archival records found.*\n\n")
        } else {
            secondaryItems.forEachIndexed { idx, item ->
                sb.append("### ${idx + 1}. ${item.title}\n")
                sb.append("- **Direct Link**: [${item.directUrl}](${item.directUrl})\n")
                sb.append("- **Source Provider**: ${item.sourceProvider} | **Type**: [${item.fileType.extension}]\n")
                sb.append("- **Classification**: ${item.classification.label}\n")
                sb.append("- **Excerpt**: ${item.verbatimExcerpt}\n\n")
            }
        }

        sb.append("## 3. Discovered Leads & Query Lineage\n\n")
        leads.forEach { lead ->
            sb.append("- [${lead.status}] **`${lead.queryPhrase}`** (${lead.rationale})\n")
        }

        return sb.toString()
    }

    fun generateJsonDossier(): String {
        val evidence = evidenceList.value
        val leads = leadsList.value
        val logs = searchLogs.value
        val currentId = _selectedInvestigationId.value ?: "N/A"
        val prog = progress.value
        val filters = searchFilters.value

        val root = org.json.JSONObject()
        root.put("app", "LeadTrace DeepSearch Bot")
        root.put("version", "1.0")
        root.put("exportTimestamp", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))
        root.put("investigationId", currentId)
        root.put("searchObjective", prog.currentQuery)

        // Statistics
        val statsObj = org.json.JSONObject().apply {
            put("queriesUsed", prog.queriesUsed)
            put("maxBudget", prog.maxBudget)
            put("totalEvidenceCount", evidence.size)
            put("instructionalCount", evidence.count { it.classification == RelevanceClassification.INSTRUCTIONAL })
            put("primarySourceCount", evidence.count { it.classification == RelevanceClassification.PRIMARY_SOURCE })
            put("status", prog.status.name)
        }
        root.put("statistics", statsObj)

        // Filter configuration
        val filtersObj = org.json.JSONObject().apply {
            put("strategyFocus", filters.strategyFocus.name)
            put("strategyFocusLabel", filters.strategyFocus.label)
            put("dateRange", filters.dateRange.name)
            put("dateRangeLabel", filters.dateRange.label)
            put("fileType", filters.fileType.name)
            put("fileTypeLabel", filters.fileType.label)
            put("domainRestriction", filters.domainRestriction.name)
            put("customDomain", filters.customDomain)
            put("summary", filters.toDisplaySummary())
        }
        root.put("searchParameters", filtersObj)

        // Ranked Evidence list
        val evidenceArray = org.json.JSONArray()
        evidence.forEach { item ->
            val evObj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("directUrl", item.directUrl)
                put("sourceProvider", item.sourceProvider)
                put("sourceDate", item.sourceDate)
                put("classification", item.classification.name)
                put("classificationLabel", item.classification.label)
                put("verificationStatus", item.verificationStatus.name)
                put("verificationStatusLabel", item.verificationStatus.label)
                put("confidenceScore", item.confidenceScore)
                put("fileType", item.fileType.name)
                put("verbatimExcerpt", item.verbatimExcerpt)
                put("discoveryQuery", item.discoveryQuery)
                put("isDuplicate", item.isDuplicate)
                put("timestamp", item.timestamp)
            }
            evidenceArray.put(evObj)
        }
        root.put("rankedEvidence", evidenceArray)

        // Discovered Leads Queue
        val leadsArray = org.json.JSONArray()
        leads.forEach { lead ->
            val lObj = org.json.JSONObject().apply {
                put("id", lead.id)
                put("queryPhrase", lead.queryPhrase)
                put("rationale", lead.rationale)
                put("leadType", lead.leadType)
                put("priority", lead.priority)
                put("status", lead.status)
                put("parentQuery", lead.parentQuery ?: "")
            }
            leadsArray.put(lObj)
        }
        root.put("discoveredLeadsQueue", leadsArray)

        // Audit Trail Logs
        val logsArray = org.json.JSONArray()
        logs.forEach { log ->
            val lg = org.json.JSONObject().apply {
                put("roundNumber", log.roundNumber)
                put("provider", log.provider)
                put("queryExecuted", log.queryExecuted)
                put("resultsCount", log.resultsCount)
                put("message", log.message)
                put("timestamp", log.timestamp)
            }
            logsArray.put(lg)
        }
        root.put("auditLogs", logsArray)

        return root.toString(2)
    }

    fun saveDossierToUri(contentResolver: android.content.ContentResolver, uri: android.net.Uri, jsonContent: String): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonContent.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
