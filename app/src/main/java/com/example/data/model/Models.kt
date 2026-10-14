package com.example.data.model

import java.util.UUID

enum class RelevanceClassification(val label: String, val description: String) {
    INSTRUCTIONAL("Instructional", "Direct guides, manuals, tutorials, and how-to documentation"),
    PRIMARY_SOURCE("Primary Source", "Original whitepapers, source code, data dumps, and foundational specs"),
    REFERENCE("Reference", "Encyclopedic entries, documentation tables, and formal standards"),
    HISTORICAL("Historical", "Archived web captures, historical records, and legacy releases"),
    NEWS("News / Event", "Secondary reporting and news coverage of an event"),
    OPINION("Opinion / Forum", "Commentary, editorial opinions, or informal discussion"),
    UNVERIFIED("Unverified Lead", "Speculative lead or unconfirmed claim requiring verification")
}

enum class VerificationStatus(val label: String) {
    VERIFIED_PRIMARY("Verified Primary"),
    VERIFIED_REFERENCE("Verified Citation"),
    UNVERIFIED_CLAIM("Unverified Claim"),
    INACCESSIBLE("Inaccessible Source")
}

enum class FileTypeBadge(val extension: String) {
    PDF("PDF"),
    DOC("DOC"),
    CODE("CODE"),
    DATASET("DATASET"),
    TXT("TXT"),
    ARCHIVE("ARCHIVE"),
    WEB("WEB")
}

enum class InvestigationStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class Investigation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val initialQuery: String,
    val maxBudget: Int = 20,
    val queriesUsed: Int = 0,
    val status: InvestigationStatus = InvestigationStatus.IDLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class EvidenceItem(
    val id: String = UUID.randomUUID().toString(),
    val investigationId: String,
    val title: String,
    val directUrl: String,
    val sourceProvider: String, // "Internet Archive", "GitHub", "Reddit", "Wikipedia", "Firecrawl/Web"
    val classification: RelevanceClassification,
    val verificationStatus: VerificationStatus,
    val confidenceScore: Int, // 0 - 100
    val sourceDate: String,
    val verbatimExcerpt: String,
    val discoveryQuery: String,
    val fileType: FileTypeBadge = FileTypeBadge.WEB,
    val isDuplicate: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchLead(
    val id: String = UUID.randomUUID().toString(),
    val investigationId: String,
    val queryPhrase: String,
    val rationale: String,
    val leadType: String, // "TERMINOLOGY", "CITATION", "FILENAME", "ALIAS", "SOURCE_OPERATOR"
    val priority: Int = 1, // 1 to 5
    val status: String = "PENDING", // PENDING, INVESTIGATING, SEARCHED, DISCARDED
    val parentQuery: String? = null
)

data class SearchLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val investigationId: String,
    val roundNumber: Int,
    val queryExecuted: String,
    val provider: String,
    val resultsCount: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RawSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val provider: String,
    val date: String? = null,
    val detectedType: FileTypeBadge = FileTypeBadge.WEB
)

enum class DateRangeFilter(val label: String, val startYear: Int?, val endYear: Int?) {
    ALL_TIME("All Time", null, null),
    AFTER_2020("After 2020", 2020, null),
    BETWEEN_2000_2020("2000 – 2020", 2000, 2020),
    NINETEENTH_CENTURY("19th Century (1800–1899)", 1800, 1899),
    TWENTIETH_CENTURY("20th Century (1900–1999)", 1900, 1999),
    CUSTOM("Custom Year", null, null)
}

enum class FileTypeFilter(val label: String, val extensions: List<String>) {
    ALL("All Types", emptyList()),
    PDF_ONLY("PDF Only", listOf("pdf")),
    DOCUMENTS("Documents (PDF, DOCX)", listOf("pdf", "docx", "doc", "odt")),
    DATASETS("Datasets (CSV, JSON)", listOf("csv", "json", "sqlite", "tsv")),
    CODE_SCRIPTS("Code (Python, C, Shell)", listOf("py", "c", "cpp", "sh", "go", "rs", "asm")),
    TEXT_ARCHIVE("Plain Text (TXT, MD)", listOf("txt", "md"))
}

enum class DomainRestrictionFilter(val label: String, val domain: String?) {
    ALL("All Domains", null),
    GOV_ONLY(".gov (Government)", "gov"),
    EDU_ONLY(".edu (Academic)", "edu"),
    ORG_ONLY(".org (Non-Profit/Archives)", "org"),
    MIL_ONLY(".mil (Military Records)", "mil"),
    CUSTOM("Custom Domain", null)
}

enum class StrategyFocus(val label: String, val description: String, val seedKeywordBonus: String) {
    GENERAL("General", "Unrestricted exploration across all categories", ""),
    HISTORICAL_19TH("19th Century Archives", "Original manuscripts, letters, registries, and 1800s records", "19th century 1800s original manuscript letters historical archive record"),
    CLIMATE_DATASETS("Climate Datasets", "Atmospheric readings, emissions tables, temperature anomalies, and public data dumps", "climate change global temperature atmospheric emissions dataset filetype:csv public data"),
    TECHNICAL_LABS("Security & Lab Specs", "Controlled research labs, protocol specifications, and implementation manuals", "specification protocol implementation guide technical laboratory manual walkthrough"),
    LEGAL_DECLASSIFIED("Declassified & Civic", "Official declassified reports, court records, hearing transcripts, and public disclosures", "declassified testimony official transcript hearing record public disclosure")
}

data class SearchFilters(
    val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val customStartYear: Int? = null,
    val customEndYear: Int? = null,
    val fileType: FileTypeFilter = FileTypeFilter.ALL,
    val domainRestriction: DomainRestrictionFilter = DomainRestrictionFilter.ALL,
    val customDomain: String = "",
    val strategyFocus: StrategyFocus = StrategyFocus.GENERAL
) {
    fun isActive(): Boolean {
        return dateRange != DateRangeFilter.ALL_TIME ||
                fileType != FileTypeFilter.ALL ||
                domainRestriction != DomainRestrictionFilter.ALL ||
                customDomain.isNotBlank() ||
                strategyFocus != StrategyFocus.GENERAL
    }

    fun toDisplaySummary(): String {
        val parts = mutableListOf<String>()
        if (strategyFocus != StrategyFocus.GENERAL) parts.add(strategyFocus.label)
        if (dateRange != DateRangeFilter.ALL_TIME) {
            if (dateRange == DateRangeFilter.CUSTOM && customStartYear != null) {
                parts.add("${customStartYear}-${customEndYear ?: "Present"}")
            } else {
                parts.add(dateRange.label)
            }
        }
        if (fileType != FileTypeFilter.ALL) parts.add(fileType.label)
        if (domainRestriction != DomainRestrictionFilter.ALL) {
            parts.add(domainRestriction.label)
        } else if (customDomain.isNotBlank()) {
            parts.add("site:$customDomain")
        }
        return if (parts.isEmpty()) "Standard Filters" else parts.joinToString(" • ")
    }

    /**
     * Appends search engine query operators (filetype:, site:, etc.)
     */
    fun applyToGeneralQuery(baseQuery: String): String {
        val sb = StringBuilder(baseQuery)

        // Strategy focus keywords
        if (strategyFocus.seedKeywordBonus.isNotBlank() && !baseQuery.contains(strategyFocus.seedKeywordBonus.take(15))) {
            sb.append(" ").append(strategyFocus.seedKeywordBonus.split(" ").take(3).joinToString(" "))
        }

        // File types
        if (fileType != FileTypeFilter.ALL && fileType.extensions.isNotEmpty()) {
            if (fileType.extensions.size == 1) {
                sb.append(" filetype:").append(fileType.extensions.first())
            } else {
                val exts = fileType.extensions.joinToString(" OR ") { "filetype:$it" }
                sb.append(" (").append(exts).append(")")
            }
        }

        // Domain restriction
        if (domainRestriction != DomainRestrictionFilter.ALL && domainRestriction.domain != null) {
            sb.append(" site:.").append(domainRestriction.domain)
        } else if (customDomain.isNotBlank()) {
            sb.append(" site:").append(customDomain)
        }

        // Date range query hint
        val start = if (dateRange == DateRangeFilter.CUSTOM) customStartYear else dateRange.startYear
        val end = if (dateRange == DateRangeFilter.CUSTOM) customEndYear else dateRange.endYear
        if (start != null && end != null) {
            sb.append(" ($start..$end)")
        } else if (start != null) {
            sb.append(" after:$start")
        }

        return sb.toString().trim()
    }

    /**
     * Builds Internet Archive Lucene query qualifiers
     */
    fun buildArchiveDateClause(): String? {
        val start = if (dateRange == DateRangeFilter.CUSTOM) customStartYear else dateRange.startYear
        val end = if (dateRange == DateRangeFilter.CUSTOM) customEndYear else dateRange.endYear

        return when {
            start != null && end != null -> "date:[$start-01-01 TO $end-12-31]"
            start != null -> "date:[$start-01-01 TO 2030-12-31]"
            end != null -> "date:[1000-01-01 TO $end-12-31]"
            else -> null
        }
    }

    fun buildArchiveFormatClause(): String? {
        return when (fileType) {
            FileTypeFilter.PDF_ONLY -> "format:(PDF)"
            FileTypeFilter.DOCUMENTS -> "format:(PDF OR Text OR Word)"
            FileTypeFilter.DATASETS -> "format:(JSON OR CSV OR Data)"
            FileTypeFilter.CODE_SCRIPTS -> "mediatype:(software)"
            FileTypeFilter.TEXT_ARCHIVE -> "format:(Text OR Plain)"
            FileTypeFilter.ALL -> null
        }
    }
}

