package com.example.domain

import com.example.data.ai.GeminiResearchAgent
import com.example.data.local.AppDatabase
import com.example.data.local.EvidenceEntity
import com.example.data.local.InvestigationEntity
import com.example.data.local.LeadEntity
import com.example.data.local.SearchLogEntity
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.data.model.SearchLead
import com.example.data.model.SearchLogEntry
import com.example.data.model.SearchFilters
import com.example.data.remote.GitHubSearchProvider
import com.example.data.remote.InternetArchiveProvider
import com.example.data.remote.RedditSearchProvider
import com.example.data.remote.SearchProvider
import com.example.data.remote.WikipediaProvider
import com.example.data.remote.FirecrawlWebProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class EngineProgress(
    val activeInvestigationId: String? = null,
    val currentQuery: String = "",
    val queriesUsed: Int = 0,
    val maxBudget: Int = 20,
    val status: InvestigationStatus = InvestigationStatus.IDLE,
    val currentProvider: String = "",
    val statusMessage: String = "Ready to investigate",
    val activeFilterSummary: String = "Standard Filters"
)

class InvestigationEngine(
    private val database: AppDatabase,
    val researchAgent: GeminiResearchAgent = GeminiResearchAgent()
) {
    private val engineScope = CoroutineScope(Dispatchers.IO)
    private var workerJob: Job? = null

    private val _progress = MutableStateFlow(EngineProgress())
    val progress: StateFlow<EngineProgress> = _progress.asStateFlow()

    var activeFilters: SearchFilters = SearchFilters()

    // Configurable providers
    val internetArchiveProvider = InternetArchiveProvider()
    val gitHubProvider = GitHubSearchProvider()
    val redditProvider = RedditSearchProvider()
    val wikipediaProvider = WikipediaProvider()
    val firecrawlProvider = FirecrawlWebProvider()

    // Active provider list
    var enabledProviders = mutableListOf<SearchProvider>(
        internetArchiveProvider,
        gitHubProvider,
        redditProvider,
        wikipediaProvider,
        firecrawlProvider
    )

    // In-memory visited URL set for fast deduplication
    private val visitedUrls = ConcurrentHashMap.newKeySet<String>()

    fun startInvestigation(
        query: String,
        budget: Int = 20,
        filters: SearchFilters = SearchFilters(),
        enableArchive: Boolean = true,
        enableGithub: Boolean = true,
        enableReddit: Boolean = true,
        enableWikipedia: Boolean = true,
        enableFirecrawl: Boolean = true
    ): String {
        // Stop any running investigation
        stopInvestigation()
        activeFilters = filters

        // Configure providers
        enabledProviders.clear()
        if (enableArchive) enabledProviders.add(internetArchiveProvider)
        if (enableGithub) enabledProviders.add(gitHubProvider)
        if (enableReddit) enabledProviders.add(redditProvider)
        if (enableWikipedia) enabledProviders.add(wikipediaProvider)
        if (enableFirecrawl) enabledProviders.add(firecrawlProvider)
        if (enabledProviders.isEmpty()) enabledProviders.add(internetArchiveProvider)

        val investigation = Investigation(
            title = query.take(50),
            initialQuery = query,
            maxBudget = budget,
            queriesUsed = 0,
            status = InvestigationStatus.RUNNING
        )

        visitedUrls.clear()

        _progress.value = EngineProgress(
            activeInvestigationId = investigation.id,
            currentQuery = query,
            queriesUsed = 0,
            maxBudget = budget,
            status = InvestigationStatus.RUNNING,
            statusMessage = "Initializing research directive...",
            activeFilterSummary = filters.toDisplaySummary()
        )

        workerJob = engineScope.launch {
            try {
                // 1. Save investigation record
                database.investigationDao().insertOrUpdate(InvestigationEntity.fromDomain(investigation))

                // 2. Log startup
                addLog(
                    investigationId = investigation.id,
                    roundNumber = 0,
                    query = query,
                    provider = "DEEPSEARCH CORE",
                    count = 0,
                    message = "Research directive initiated: \"$query\" (Budget: $budget queries) [Filters: ${filters.toDisplaySummary()}]"
                )

                // 3. Generate initial seed leads using research directive & filters
                val initialLeads = researchAgent.generateInitialLeads(investigation.id, query, filters)
                database.leadDao().insertAll(initialLeads.map { LeadEntity.fromDomain(it) })

                addLog(
                    investigationId = investigation.id,
                    roundNumber = 1,
                    query = query,
                    provider = "Gemini Lead Generator",
                    count = initialLeads.size,
                    message = "Generated ${initialLeads.size} initial strategies focusing on: ${filters.strategyFocus.label}."
                )

                // 4. Run autonomous loop
                runSearchLoop(investigation.id, budget, filters)

            } catch (e: CancellationException) {
                updateInvestigationStatus(investigation.id, InvestigationStatus.PAUSED)
                _progress.value = _progress.value.copy(
                    status = InvestigationStatus.PAUSED,
                    statusMessage = "Investigation paused by user."
                )
            } catch (e: Exception) {
                updateInvestigationStatus(investigation.id, InvestigationStatus.ERROR)
                _progress.value = _progress.value.copy(
                    status = InvestigationStatus.ERROR,
                    statusMessage = "Engine error: ${e.localizedMessage ?: "Unknown"}"
                )
            }
        }

        return investigation.id
    }

    private suspend fun runSearchLoop(investigationId: String, maxBudget: Int, filters: SearchFilters = activeFilters) {
        var queriesExecuted = 0
        var round = 1

        while (queriesExecuted < maxBudget && engineScope.isActive) {
            // Check for pause state
            if (_progress.value.status == InvestigationStatus.PAUSED) {
                delay(500)
                continue
            }

            // Pop next highest priority pending lead
            val pendingLeads = database.leadDao().getPendingLeads(investigationId)
            if (pendingLeads.isEmpty()) {
                addLog(
                    investigationId = investigationId,
                    roundNumber = round,
                    query = "N/A",
                    provider = "LEAD QUEUE",
                    count = 0,
                    message = "No further pending leads. Research cycle complete."
                )
                break
            }

            val currentLead = pendingLeads.first()
            database.leadDao().updateStatus(currentLead.id, "INVESTIGATING")

            _progress.value = _progress.value.copy(
                currentQuery = currentLead.queryPhrase,
                queriesUsed = queriesExecuted + 1,
                statusMessage = "Executing lead [Round $round]: \"${currentLead.queryPhrase}\""
            )

            // Select provider in round-robin fashion or distribute
            val provider = enabledProviders[(queriesExecuted % enabledProviders.size)]
            _progress.value = _progress.value.copy(currentProvider = provider.name)

            addLog(
                investigationId = investigationId,
                roundNumber = round,
                query = currentLead.queryPhrase,
                provider = provider.name,
                count = 0,
                message = "Querying ${provider.name} for: \"${currentLead.queryPhrase}\""
            )

            // Execute search with user filters
            val rawResults = try {
                provider.search(currentLead.queryPhrase, maxResults = 8, filters = filters)
            } catch (e: Exception) {
                emptyList()
            }

            queriesExecuted++
            database.leadDao().updateStatus(currentLead.id, "SEARCHED")

            var newDiscoveriesCount = 0
            val candidateLeads = mutableListOf<SearchLead>()

            for (raw in rawResults) {
                val isDup = visitedUrls.contains(raw.url) || database.evidenceDao().existsUrl(investigationId, raw.url)
                if (!isDup) {
                    visitedUrls.add(raw.url)
                }

                // Analyze discovery with Gemini / Heuristic agent
                val analyzed = researchAgent.analyzeResult(
                    investigationId = investigationId,
                    result = raw,
                    executingQuery = currentLead.queryPhrase,
                    isDuplicate = isDup
                )

                // Save evidence
                database.evidenceDao().insert(EvidenceEntity.fromDomain(analyzed.evidence))
                newDiscoveriesCount++

                // Collect newly extracted terminology / leads if not duplicate
                if (!isDup && analyzed.newLeads.isNotEmpty()) {
                    candidateLeads.addAll(analyzed.newLeads)
                }
            }

            // Enqueue new leads
            if (candidateLeads.isNotEmpty()) {
                database.leadDao().insertAll(candidateLeads.take(4).map { LeadEntity.fromDomain(it) })
            }

            addLog(
                investigationId = investigationId,
                roundNumber = round,
                query = currentLead.queryPhrase,
                provider = provider.name,
                count = newDiscoveriesCount,
                message = "Found $newDiscoveriesCount results (${candidateLeads.size} new leads extracted). Budget remaining: ${maxBudget - queriesExecuted} queries."
            )

            // Update investigation in DB
            val currentInv = database.investigationDao().getInvestigationById(investigationId)
            if (currentInv != null) {
                database.investigationDao().update(
                    currentInv.copy(
                        queriesUsed = queriesExecuted,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            round++
            // Cooperative pause between queries for rate limiting and UI rendering
            delay(800)
        }

        // Finish investigation
        val finalStatus = InvestigationStatus.COMPLETED
        updateInvestigationStatus(investigationId, finalStatus)

        _progress.value = _progress.value.copy(
            status = finalStatus,
            queriesUsed = queriesExecuted,
            statusMessage = "Investigation complete. Budget satisfied ($queriesExecuted/$maxBudget queries used)."
        )

        addLog(
            investigationId = investigationId,
            roundNumber = round,
            query = "COMPLETED",
            provider = "DEEPSEARCH CORE",
            count = 0,
            message = "Investigation finished. Evidence dossier compiled and ranked."
        )
    }

    fun pauseInvestigation() {
        val current = _progress.value
        if (current.status == InvestigationStatus.RUNNING) {
            _progress.value = current.copy(
                status = InvestigationStatus.PAUSED,
                statusMessage = "Investigation paused by user."
            )
            current.activeInvestigationId?.let { id ->
                engineScope.launch {
                    updateInvestigationStatus(id, InvestigationStatus.PAUSED)
                }
            }
        }
    }

    fun resumeInvestigation() {
        val current = _progress.value
        if (current.status == InvestigationStatus.PAUSED && current.activeInvestigationId != null) {
            _progress.value = current.copy(
                status = InvestigationStatus.RUNNING,
                statusMessage = "Resuming investigation loop..."
            )
            val invId = current.activeInvestigationId
            workerJob = engineScope.launch {
                updateInvestigationStatus(invId, InvestigationStatus.RUNNING)
                runSearchLoop(invId, current.maxBudget, activeFilters)
            }
        }
    }

    fun stopInvestigation() {
        workerJob?.cancel()
        workerJob = null
        _progress.value.activeInvestigationId?.let { id ->
            engineScope.launch {
                updateInvestigationStatus(id, InvestigationStatus.COMPLETED)
            }
        }
        _progress.value = _progress.value.copy(
            status = InvestigationStatus.IDLE,
            statusMessage = "Investigation stopped."
        )
    }

    fun addManualLead(investigationId: String, queryPhrase: String) {
        engineScope.launch {
            val lead = SearchLead(
                investigationId = investigationId,
                queryPhrase = queryPhrase,
                rationale = "User-injected target lead",
                leadType = "MANUAL_INJECTION",
                priority = 5
            )
            database.leadDao().insert(LeadEntity.fromDomain(lead))
            addLog(
                investigationId = investigationId,
                roundNumber = 0,
                query = queryPhrase,
                provider = "User Directive",
                count = 1,
                message = "Injected priority manual lead: \"$queryPhrase\""
            )
        }
    }

    private suspend fun addLog(
        investigationId: String,
        roundNumber: Int,
        query: String,
        provider: String,
        count: Int,
        message: String
    ) {
        val log = SearchLogEntry(
            investigationId = investigationId,
            roundNumber = roundNumber,
            queryExecuted = query,
            provider = provider,
            resultsCount = count,
            message = message
        )
        database.searchLogDao().insert(SearchLogEntity.fromDomain(log))
    }

    private suspend fun updateInvestigationStatus(id: String, status: InvestigationStatus) {
        val inv = database.investigationDao().getInvestigationById(id)
        if (inv != null) {
            database.investigationDao().update(
                inv.copy(
                    status = status.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
