package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FileTypeBadge
import com.example.data.model.Investigation
import com.example.data.model.InvestigationStatus
import com.example.data.model.RelevanceClassification
import com.example.data.model.VerificationStatus
import com.example.data.model.EvidenceItem
import com.example.data.model.SearchLead
import com.example.data.model.SearchLogEntry

@Entity(tableName = "investigations")
data class InvestigationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val initialQuery: String,
    val maxBudget: Int,
    val queriesUsed: Int,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Investigation = Investigation(
        id = id,
        title = title,
        initialQuery = initialQuery,
        maxBudget = maxBudget,
        queriesUsed = queriesUsed,
        status = try { InvestigationStatus.valueOf(status) } catch (e: Exception) { InvestigationStatus.IDLE },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: Investigation): InvestigationEntity = InvestigationEntity(
            id = domain.id,
            title = domain.title,
            initialQuery = domain.initialQuery,
            maxBudget = domain.maxBudget,
            queriesUsed = domain.queriesUsed,
            status = domain.status.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

@Entity(tableName = "evidence_items")
data class EvidenceEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val title: String,
    val directUrl: String,
    val sourceProvider: String,
    val classification: String,
    val verificationStatus: String,
    val confidenceScore: Int,
    val sourceDate: String,
    val verbatimExcerpt: String,
    val discoveryQuery: String,
    val fileType: String,
    val isDuplicate: Boolean,
    val timestamp: Long
) {
    fun toDomain(): EvidenceItem = EvidenceItem(
        id = id,
        investigationId = investigationId,
        title = title,
        directUrl = directUrl,
        sourceProvider = sourceProvider,
        classification = try { RelevanceClassification.valueOf(classification) } catch (e: Exception) { RelevanceClassification.UNVERIFIED },
        verificationStatus = try { VerificationStatus.valueOf(verificationStatus) } catch (e: Exception) { VerificationStatus.UNVERIFIED_CLAIM },
        confidenceScore = confidenceScore,
        sourceDate = sourceDate,
        verbatimExcerpt = verbatimExcerpt,
        discoveryQuery = discoveryQuery,
        fileType = try { FileTypeBadge.valueOf(fileType) } catch (e: Exception) { FileTypeBadge.WEB },
        isDuplicate = isDuplicate,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(domain: EvidenceItem): EvidenceEntity = EvidenceEntity(
            id = domain.id,
            investigationId = domain.investigationId,
            title = domain.title,
            directUrl = domain.directUrl,
            sourceProvider = domain.sourceProvider,
            classification = domain.classification.name,
            verificationStatus = domain.verificationStatus.name,
            confidenceScore = domain.confidenceScore,
            sourceDate = domain.sourceDate,
            verbatimExcerpt = domain.verbatimExcerpt,
            discoveryQuery = domain.discoveryQuery,
            fileType = domain.fileType.name,
            isDuplicate = domain.isDuplicate,
            timestamp = domain.timestamp
        )
    }
}

@Entity(tableName = "search_leads")
data class LeadEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val queryPhrase: String,
    val rationale: String,
    val leadType: String,
    val priority: Int,
    val status: String,
    val parentQuery: String?
) {
    fun toDomain(): SearchLead = SearchLead(
        id = id,
        investigationId = investigationId,
        queryPhrase = queryPhrase,
        rationale = rationale,
        leadType = leadType,
        priority = priority,
        status = status,
        parentQuery = parentQuery
    )

    companion object {
        fun fromDomain(domain: SearchLead): LeadEntity = LeadEntity(
            id = domain.id,
            investigationId = domain.investigationId,
            queryPhrase = domain.queryPhrase,
            rationale = domain.rationale,
            leadType = domain.leadType,
            priority = domain.priority,
            status = domain.status,
            parentQuery = domain.parentQuery
        )
    }
}

@Entity(tableName = "search_logs")
data class SearchLogEntity(
    @PrimaryKey val id: String,
    val investigationId: String,
    val roundNumber: Int,
    val queryExecuted: String,
    val provider: String,
    val resultsCount: Int,
    val message: String,
    val timestamp: Long
) {
    fun toDomain(): SearchLogEntry = SearchLogEntry(
        id = id,
        investigationId = investigationId,
        roundNumber = roundNumber,
        queryExecuted = queryExecuted,
        provider = provider,
        resultsCount = resultsCount,
        message = message,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(domain: SearchLogEntry): SearchLogEntity = SearchLogEntity(
            id = domain.id,
            investigationId = domain.investigationId,
            roundNumber = domain.roundNumber,
            queryExecuted = domain.queryExecuted,
            provider = domain.provider,
            resultsCount = domain.resultsCount,
            message = domain.message,
            timestamp = domain.timestamp
        )
    }
}
