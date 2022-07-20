package com.wa2c.android.cifsdocumentsprovider.data.db

import androidx.paging.PagingSource
import androidx.room.*

@Dao
internal interface ConnectionSettingDao {

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME}")
    suspend fun getList(): List<ConnectionSettingEntity>

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE id = :id")
    suspend fun getEntity(id: String): ConnectionSettingEntity?

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} ORDER BY sort_order")
    fun getPagingSource(): PagingSource<Int, ConnectionSettingEntity>

    @Query("SELECT max(sort_order) FROM ${ConnectionSettingEntity.TABLE_NAME}")
    fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConnectionSettingEntity)

    @Query("DELETE FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE id = :id")
    suspend fun delete(id: String)

    @Transaction
    suspend fun update(insertEntity: ConnectionSettingEntity, deleteId: String?) {
        if (deleteId != null && insertEntity.id != deleteId) {
            delete(deleteId)
        }
        insert(insertEntity)
    }

}