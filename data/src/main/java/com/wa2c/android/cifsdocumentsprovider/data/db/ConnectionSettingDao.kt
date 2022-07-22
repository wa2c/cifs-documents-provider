package com.wa2c.android.cifsdocumentsprovider.data.db

import androidx.paging.PagingSource
import androidx.room.*

@Dao
internal interface ConnectionSettingDao {

    @Query("SELECT count(id) FROM ${ConnectionSettingEntity.TABLE_NAME}")
    suspend fun getCount(): Int

    @Query("SELECT max(sort_order) FROM ${ConnectionSettingEntity.TABLE_NAME}")
    suspend fun getMaxSortOrder(): Int

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE id = :id")
    suspend fun getEntity(id: String): ConnectionSettingEntity?

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE instr(:uri, uri) > 0 ORDER BY sort_order" )
    suspend fun getEntityByUri(uri: String): ConnectionSettingEntity?

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} ORDER BY sort_order")
    suspend fun getList(): List<ConnectionSettingEntity>

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} ORDER BY sort_order")
    fun getPagingSource(): PagingSource<Int, ConnectionSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConnectionSettingEntity)

    @Query("DELETE FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE ${ConnectionSettingEntity.TABLE_NAME} SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)



    @Transaction
    suspend fun move(fromPosition: Int, toPosition: Int) {
        val list = getList().toMutableList()
        list.add(toPosition, list.removeAt(fromPosition))
        list.forEachIndexed { index, entity ->
            updateSortOrder(entity.id, index + 1)
        }
    }

}