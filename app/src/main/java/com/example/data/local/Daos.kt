package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestigationDao {
    @Query("SELECT * FROM investigations ORDER BY updatedAt DESC")
    fun getAllInvestigations(): Flow<List<InvestigationEntity>>

    @Query("SELECT * FROM investigations WHERE id = :id LIMIT 1")
    suspend fun getInvestigationById(id: String): InvestigationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(investigation: InvestigationEntity)

    @Update
    suspend fun update(investigation: InvestigationEntity)

    @Query("DELETE FROM investigations WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence_items WHERE investigationId = :investigationId ORDER BY confidenceScore DESC, timestamp DESC")
    fun getEvidenceForInvestigation(investigationId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_items WHERE investigationId = :investigationId")
    suspend fun getEvidenceList(investigationId: String): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: EvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evidenceList: List<EvidenceEntity>)

    @Query("SELECT COUNT(*) FROM evidence_items WHERE investigationId = :investigationId")
    suspend fun getCount(investigationId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM evidence_items WHERE investigationId = :investigationId AND directUrl = :url)")
    suspend fun existsUrl(investigationId: String, url: String): Boolean
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM search_leads WHERE investigationId = :investigationId ORDER BY priority DESC")
    fun getLeadsForInvestigation(investigationId: String): Flow<List<LeadEntity>>

    @Query("SELECT * FROM search_leads WHERE investigationId = :investigationId AND status = 'PENDING' ORDER BY priority DESC")
    suspend fun getPendingLeads(investigationId: String): List<LeadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lead: LeadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leads: List<LeadEntity>)

    @Query("UPDATE search_leads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM search_leads WHERE id = :id")
    suspend fun deleteLead(id: String)
}

@Dao
interface SearchLogDao {
    @Query("SELECT * FROM search_logs WHERE investigationId = :investigationId ORDER BY timestamp ASC")
    fun getLogsForInvestigation(investigationId: String): Flow<List<SearchLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SearchLogEntity)

    @Query("DELETE FROM search_logs WHERE investigationId = :investigationId")
    suspend fun clearLogs(investigationId: String)
}
