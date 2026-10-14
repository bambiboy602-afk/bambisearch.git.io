package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.EvidenceItem
import com.example.data.model.FileTypeBadge
import com.example.data.model.RawSearchResult
import com.example.data.model.RelevanceClassification
import com.example.data.model.SearchLead
import com.example.data.model.VerificationStatus
import com.example.data.model.SearchFilters
import com.example.data.model.StrategyFocus
import com.example.data.model.FileTypeFilter
import com.example.data.model.DomainRestrictionFilter
import com.example.data.remote.SearchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.regex.Pattern

data class AnalyzedDiscovery(
    val evidence: EvidenceItem,
    val newLeads: List<SearchLead>
)

class GeminiResearchAgent(
    private var customApiKey: String = ""
) {
    fun updateApiKey(key: String) {
        customApiKey = key
    }

    private fun getEffectiveApiKey(): String {
        if (customApiKey.isNotBlank() && customApiKey != "MY_GEMINI_API_KEY") {
            return customApiKey
        }
        val buildConfigKey = BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    /**
     * Generates initial alternate search queries, historical terms, and source operators
     * based on the research directive and user-specified search filters.
     */
    fun generateInitialLeads(
        investigationId: String,
        seedQuery: String,
        filters: SearchFilters = SearchFilters()
    ): List<SearchLead> {
        val leads = mutableListOf<SearchLead>()

        // 1. Primary Seed Lead (with filter operators applied)
        val filteredSeed = filters.applyToGeneralQuery(seedQuery)
        leads.add(
            SearchLead(
                investigationId = investigationId,
                queryPhrase = filteredSeed,
                rationale = "Direct inquiry with active filters: ${filters.toDisplaySummary()}",
                leadType = "PRIMARY_SEED",
                priority = 5
            )
        )

        // 2. Strategy-Specific Focus Leads
        when (filters.strategyFocus) {
            StrategyFocus.HISTORICAL_19TH -> {
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery 19th century primary source archive 1800s",
                        rationale = "Historical archive focus: 19th century original records",
                        leadType = "HISTORICAL_FOCUS",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery original manuscript letters records correspondence 1800..1899",
                        rationale = "Original manuscripts and letters from 1800s",
                        leadType = "HISTORICAL_FOCUS",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
            }
            StrategyFocus.CLIMATE_DATASETS -> {
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery global climate atmospheric temperature dataset filetype:csv",
                        rationale = "Public scientific datasets and atmospheric records",
                        leadType = "DATASET_FOCUS",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery emissions observations public data dump table",
                        rationale = "Observational records and public climate tables",
                        leadType = "DATASET_FOCUS",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
            }
            StrategyFocus.TECHNICAL_LABS -> {
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery manual guide specification protocol walkthrough",
                        rationale = "Technical documentation and controlled lab walkthroughs",
                        leadType = "LAB_FOCUS",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery source code implementation example lab",
                        rationale = "Demonstration code and reproducible lab scripts",
                        leadType = "LAB_FOCUS",
                        priority = 3,
                        parentQuery = seedQuery
                    )
                )
            }
            StrategyFocus.LEGAL_DECLASSIFIED -> {
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery declassified official report transcript testimony",
                        rationale = "Official declassified reports and hearing records",
                        leadType = "CIVIC_FOCUS",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
            }
            StrategyFocus.GENERAL -> {
                leads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$seedQuery manual guide documentation tutorial",
                        rationale = "Instructional documentation and technical guides",
                        leadType = "SOURCE_OPERATOR",
                        priority = 4,
                        parentQuery = seedQuery
                    )
                )
            }
        }

        // 3. File type operator lead
        if (filters.fileType != FileTypeFilter.ALL && filters.fileType.extensions.isNotEmpty()) {
            val ext = filters.fileType.extensions.first()
            leads.add(
                SearchLead(
                    investigationId = investigationId,
                    queryPhrase = "$seedQuery filetype:$ext",
                    rationale = "Explicit file format target: .$ext",
                    leadType = "FILENAME",
                    priority = 4,
                    parentQuery = seedQuery
                )
            )
        } else {
            leads.add(
                SearchLead(
                    investigationId = investigationId,
                    queryPhrase = "$seedQuery filetype:pdf whitepaper spec",
                    rationale = "Original PDF whitepaper and specification files",
                    leadType = "FILENAME",
                    priority = 3,
                    parentQuery = seedQuery
                )
            )
        }

        // 4. Domain-restricted lead if active
        if (filters.domainRestriction != DomainRestrictionFilter.ALL && filters.domainRestriction.domain != null) {
            leads.add(
                SearchLead(
                    investigationId = investigationId,
                    queryPhrase = "$seedQuery site:.${filters.domainRestriction.domain}",
                    rationale = "Domain restriction: .${filters.domainRestriction.domain} records",
                    leadType = "DOMAIN_OPERATOR",
                    priority = 4,
                    parentQuery = seedQuery
                )
            )
        } else if (filters.customDomain.isNotBlank()) {
            leads.add(
                SearchLead(
                    investigationId = investigationId,
                    queryPhrase = "$seedQuery site:${filters.customDomain}",
                    rationale = "Domain restriction: ${filters.customDomain}",
                    leadType = "DOMAIN_OPERATOR",
                    priority = 4,
                    parentQuery = seedQuery
                )
            )
        }

        return leads
    }

    /**
     * Evaluates a search result and extracts evidence and newly discovered leads.
     */
    suspend fun analyzeResult(
        investigationId: String,
        result: RawSearchResult,
        executingQuery: String,
        isDuplicate: Boolean
    ): AnalyzedDiscovery = withContext(Dispatchers.Default) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val aiResult = analyzeWithGeminiApi(apiKey, investigationId, result, executingQuery, isDuplicate)
                if (aiResult != null) return@withContext aiResult
            } catch (e: Exception) {
                // Fallback to local heuristic extractor
            }
        }
        analyzeWithHeuristics(investigationId, result, executingQuery, isDuplicate)
    }

    /**
     * High-accuracy heuristic analyzer implementing the DEEPSEARCH BOT directive.
     */
    private fun analyzeWithHeuristics(
        investigationId: String,
        result: RawSearchResult,
        executingQuery: String,
        isDuplicate: Boolean
    ): AnalyzedDiscovery {
        val combinedText = (result.title + " " + result.snippet).lowercase(Locale.ROOT)

        // Classification heuristics
        val classification = when {
            combinedText.contains("manual") || combinedText.contains("tutorial") || combinedText.contains("how-to") ||
                    combinedText.contains("guide") || combinedText.contains("handbook") || combinedText.contains("walkthrough") ->
                RelevanceClassification.INSTRUCTIONAL

            combinedText.contains("whitepaper") || combinedText.contains("specification") || combinedText.contains("rfc") ||
                    combinedText.contains("source code") || combinedText.contains("lab") || result.detectedType == FileTypeBadge.CODE ->
                RelevanceClassification.PRIMARY_SOURCE

            combinedText.contains("archive") || combinedText.contains("historical") || combinedText.contains("legacy") ||
                    combinedText.contains("wayback") || result.provider == "Internet Archive" ->
                RelevanceClassification.HISTORICAL

            combinedText.contains("wikipedia") || combinedText.contains("encyclopedia") || combinedText.contains("dictionary") ->
                RelevanceClassification.REFERENCE

            combinedText.contains("news") || combinedText.contains("reported") || combinedText.contains("incident") ||
                    combinedText.contains("arrest") || combinedText.contains("announced") ->
                RelevanceClassification.NEWS

            combinedText.contains("forum") || combinedText.contains("opinion") || combinedText.contains("thoughts?") ->
                RelevanceClassification.OPINION

            else -> RelevanceClassification.REFERENCE
        }

        // Verification status
        val verificationStatus = when {
            isDuplicate -> VerificationStatus.UNVERIFIED_CLAIM
            result.url.startsWith("http") && (classification == RelevanceClassification.INSTRUCTIONAL || classification == RelevanceClassification.PRIMARY_SOURCE) ->
                VerificationStatus.VERIFIED_PRIMARY
            result.url.startsWith("http") -> VerificationStatus.VERIFIED_REFERENCE
            else -> VerificationStatus.INACCESSIBLE
        }

        // Confidence scoring:
        // Instructional and Primary materials get higher confidence; news/opinion get penalties.
        var score = 65
        if (classification == RelevanceClassification.INSTRUCTIONAL) score += 25
        if (classification == RelevanceClassification.PRIMARY_SOURCE) score += 20
        if (result.detectedType == FileTypeBadge.PDF || result.detectedType == FileTypeBadge.CODE) score += 10
        if (classification == RelevanceClassification.NEWS) score -= 25
        if (classification == RelevanceClassification.OPINION) score -= 15
        if (isDuplicate) score -= 20
        score = score.coerceIn(15, 98)

        // Extract excerpt
        val excerpt = if (result.snippet.isNotBlank()) {
            result.snippet.take(240)
        } else {
            "Original capture for: ${result.title}"
        }

        val evidence = EvidenceItem(
            investigationId = investigationId,
            title = result.title.ifBlank { "Discovered Document" },
            directUrl = result.url,
            sourceProvider = result.provider,
            classification = classification,
            verificationStatus = verificationStatus,
            confidenceScore = score,
            sourceDate = result.date ?: "Document Archive",
            verbatimExcerpt = excerpt,
            discoveryQuery = executingQuery,
            fileType = result.detectedType,
            isDuplicate = isDuplicate
        )

        // Extract follow-up search leads (terminology, filenames, citations)
        val extractedLeads = mutableListOf<SearchLead>()

        // 1. Look for acronyms or specialized terms in uppercase/quotes
        val termMatcher = Pattern.compile("([A-Z]{3,8}|\\b[a-zA-Z0-9_-]+\\.(?:pdf|txt|py|c|sh|json|csv|md)\\b)").matcher(result.title + " " + result.snippet)
        var count = 0
        while (termMatcher.find() && count < 2) {
            val term = termMatcher.group()
            if (!term.equals("HTTP", true) && !term.equals("JSON", true) && !term.equals("HTML", true) && term.length > 2) {
                extractedLeads.add(
                    SearchLead(
                        investigationId = investigationId,
                        queryPhrase = "$term reference specification",
                        rationale = "Extracted specialized term: $term",
                        leadType = if (term.contains(".")) "FILENAME" else "TERMINOLOGY",
                        priority = 4,
                        parentQuery = executingQuery
                    )
                )
                count++
            }
        }

        // 2. Author or repository lead
        if (result.provider == "GitHub Repositories" && result.title.contains("/")) {
            val owner = result.title.substringBefore("/")
            extractedLeads.add(
                SearchLead(
                    investigationId = investigationId,
                    queryPhrase = "user:$owner research publications",
                    rationale = "Author/organization repository lead: $owner",
                    leadType = "CITATION",
                    priority = 3,
                    parentQuery = executingQuery
                )
            )
        }

        return AnalyzedDiscovery(evidence, extractedLeads)
    }

    /**
     * Optional Gemini API integration for structured extraction when an API key is available.
     */
    private suspend fun analyzeWithGeminiApi(
        apiKey: String,
        investigationId: String,
        result: RawSearchResult,
        executingQuery: String,
        isDuplicate: Boolean
    ): AnalyzedDiscovery? = withContext(Dispatchers.IO) {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val prompt = """
            You are DEEPSEARCH BOT, an evidence-grounded research agent.
            Analyze this search result:
            Title: ${result.title}
            URL: ${result.url}
            Provider: ${result.provider}
            Date: ${result.date}
            Snippet: ${result.snippet}
            Discovery Query: $executingQuery

            Instructions:
            1. Classify relevance as: INSTRUCTIONAL, PRIMARY_SOURCE, REFERENCE, HISTORICAL, NEWS, OPINION, or UNVERIFIED.
            2. Compute confidence score 0-100 based on instructional/primary value (prioritize manuals, specs, labs; penalize secondary news/opinion).
            3. Extract up to 2 specific follow-up search phrases (specialized terminology, citations, filenames).
            
            Return JSON:
            {
               "classification": "INSTRUCTIONAL",
               "confidence": 85,
               "excerpt": "Matching verbatim text...",
               "followUpQueries": ["query 1", "query 2"]
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        SearchUtils.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null
            val text = parts.getJSONObject(0).optString("text")

            val parsed = JSONObject(text)
            val classStr = parsed.optString("classification", "REFERENCE")
            val confidence = parsed.optInt("confidence", 70)
            val excerpt = parsed.optString("excerpt", result.snippet.take(200))
            val queriesArray = parsed.optJSONArray("followUpQueries") ?: JSONArray()

            val classification = try {
                RelevanceClassification.valueOf(classStr)
            } catch (e: Exception) {
                RelevanceClassification.REFERENCE
            }

            val verification = if (isDuplicate) VerificationStatus.UNVERIFIED_CLAIM else VerificationStatus.VERIFIED_PRIMARY

            val evidence = EvidenceItem(
                investigationId = investigationId,
                title = result.title,
                directUrl = result.url,
                sourceProvider = result.provider,
                classification = classification,
                verificationStatus = verification,
                confidenceScore = confidence,
                sourceDate = result.date ?: "Live Record",
                verbatimExcerpt = excerpt,
                discoveryQuery = executingQuery,
                fileType = result.detectedType,
                isDuplicate = isDuplicate
            )

            val leads = mutableListOf<SearchLead>()
            for (i in 0 until queriesArray.length()) {
                val q = queriesArray.getString(i)
                if (q.isNotBlank()) {
                    leads.add(
                        SearchLead(
                            investigationId = investigationId,
                            queryPhrase = q,
                            rationale = "AI-suggested lead from ${result.title.take(30)}",
                            leadType = "AI_EXPANSION",
                            priority = 4,
                            parentQuery = executingQuery
                        )
                    )
                }
            }

            return@withContext AnalyzedDiscovery(evidence, leads)
        }
    }
}
