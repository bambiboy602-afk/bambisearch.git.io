package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiResearchAgent
import com.example.data.model.DateRangeFilter
import com.example.data.model.DomainRestrictionFilter
import com.example.data.model.FileTypeBadge
import com.example.data.model.FileTypeFilter
import com.example.data.model.RawSearchResult
import com.example.data.model.RelevanceClassification
import com.example.data.model.SearchFilters
import com.example.data.model.StrategyFocus
import com.example.data.remote.SearchUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LeadTrace", appName)
    }

    @Test
    fun `detect file types properly`() {
        assertEquals(FileTypeBadge.PDF, SearchUtils.detectFileType("https://archive.org/download/manual.pdf"))
        assertEquals(FileTypeBadge.CODE, SearchUtils.detectFileType("https://github.com/org/repo/blob/main/script.py"))
        assertEquals(FileTypeBadge.DATASET, SearchUtils.detectFileType("https://example.com/dump.csv"))
    }

    @Test
    fun `generate initial research leads`() {
        val agent = GeminiResearchAgent()
        val leads = agent.generateInitialLeads("test_inv_id", "buffer overflow exploit mitigation")
        assertTrue(leads.isNotEmpty())
        assertTrue(leads.any { it.queryPhrase.contains("manual") || it.queryPhrase.contains("guide") })
    }

    @Test
    fun `test advanced search filters query generation`() {
        // 1. 19th century historical filter
        val historicalFilters = SearchFilters(
            dateRange = DateRangeFilter.NINETEENTH_CENTURY,
            fileType = FileTypeFilter.PDF_ONLY,
            domainRestriction = DomainRestrictionFilter.GOV_ONLY,
            strategyFocus = StrategyFocus.HISTORICAL_19TH
        )

        assertEquals("date:[1800-01-01 TO 1899-12-31]", historicalFilters.buildArchiveDateClause())
        assertEquals("format:(PDF)", historicalFilters.buildArchiveFormatClause())

        val appliedQuery = historicalFilters.applyToGeneralQuery("civil war registry")
        assertTrue(appliedQuery.contains("filetype:pdf"))
        assertTrue(appliedQuery.contains("site:.gov"))
        assertTrue(appliedQuery.contains("1800..1899"))

        // 2. Climate dataset filter after 2020
        val climateFilters = SearchFilters(
            dateRange = DateRangeFilter.AFTER_2020,
            fileType = FileTypeFilter.DATASETS,
            domainRestriction = DomainRestrictionFilter.ALL,
            strategyFocus = StrategyFocus.CLIMATE_DATASETS
        )

        assertEquals("date:[2020-01-01 TO 2030-12-31]", climateFilters.buildArchiveDateClause())
        val climateQuery = climateFilters.applyToGeneralQuery("global temperature anomalies")
        assertTrue(climateQuery.contains("climate change") || climateQuery.contains("atmospheric"))
        assertTrue(climateQuery.contains("filetype:csv"))
        assertTrue(climateQuery.contains("after:2020"))
    }

    @Test
    fun `test strategy focus lead generation`() {
        val agent = GeminiResearchAgent()

        // 19th Century strategy
        val historicalFilters = SearchFilters(
            dateRange = DateRangeFilter.NINETEENTH_CENTURY,
            strategyFocus = StrategyFocus.HISTORICAL_19TH
        )
        val histLeads = agent.generateInitialLeads("inv_hist", "lincoln correspondence", historicalFilters)
        assertTrue(histLeads.any { it.queryPhrase.contains("19th century") || it.queryPhrase.contains("manuscript") })

        // Climate Datasets strategy
        val climateFilters = SearchFilters(
            strategyFocus = StrategyFocus.CLIMATE_DATASETS,
            fileType = FileTypeFilter.DATASETS
        )
        val climateLeads = agent.generateInitialLeads("inv_climate", "ocean heat content", climateFilters)
        assertTrue(climateLeads.any { it.queryPhrase.contains("dataset") || it.queryPhrase.contains("temperature") })
    }

    @Test
    fun `analyze search result with instructional prioritization`() = runBlocking {
        val agent = GeminiResearchAgent()
        val sample = RawSearchResult(
            title = "Hardware Security Lab: Practical Tutorial and Manual",
            url = "https://archive.org/details/lab_manual_2021",
            snippet = "A complete step-by-step walkthrough manual for hardware lab testing.",
            provider = "Internet Archive",
            date = "2021-05-10",
            detectedType = FileTypeBadge.PDF
        )

        val discovery = agent.analyzeResult(
            investigationId = "test_inv_1",
            result = sample,
            executingQuery = "hardware security lab",
            isDuplicate = false
        )

        assertEquals(RelevanceClassification.INSTRUCTIONAL, discovery.evidence.classification)
        assertTrue(discovery.evidence.confidenceScore >= 80)
        assertNotNull(discovery.evidence.directUrl)
    }

    @Test
    fun `test json dossier export structure`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.InvestigationViewModel(context)

        val jsonStr = viewModel.generateJsonDossier()
        assertNotNull(jsonStr)

        val json = org.json.JSONObject(jsonStr)
        assertEquals("LeadTrace DeepSearch Bot", json.getString("app"))
        assertTrue(json.has("statistics"))
        assertTrue(json.has("rankedEvidence"))
        assertTrue(json.has("discoveredLeadsQueue"))
        assertTrue(json.has("searchParameters"))
    }
}
