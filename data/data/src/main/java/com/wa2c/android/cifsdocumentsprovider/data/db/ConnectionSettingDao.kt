package com.wa2c.android.cifsdocumentsprovider.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface ConnectionSettingDao {

    @Query("SELECT count(id) FROM ${ConnectionSettingEntity.TABLE_NAME}")
    suspend fun getCount(): Int

    @Query("SELECT coalesce(max(sort_order), 0) FROM ${ConnectionSettingEntity.TABLE_NAME}")
    suspend fun getMaxSortOrder(): Int

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE id = :id")
    suspend fun getEntity(id: String): ConnectionSettingEntity?

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE instr(:uri, uri) > 0 ORDER BY sort_order" )
    suspend fun getEntityByUri(uri: String): ConnectionSettingEntity?

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} ORDER BY sort_order")
    fun getList(): Flow<List<ConnectionSettingEntity>>

    @Query("SELECT * FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE type IN (:types) ORDER BY sort_order")
    suspend fun getTypedList(types: Collection<String>): List<ConnectionSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConnectionSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ConnectionSettingEntity>)

    @Query("DELETE FROM ${ConnectionSettingEntity.TABLE_NAME} WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ${ConnectionSettingEntity.TABLE_NAME}")
    suspend fun deleteAll()

    @Query("UPDATE ${ConnectionSettingEntity.TABLE_NAME} SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Transaction
    suspend fun replace(entities: List<ConnectionSettingEntity>) {
        deleteAll()
        insertAll(entities)
    }

    @Transaction
    suspend fun move(fromPosition: Int, toPosition: Int) {
        val list = getList().first().toMutableList()
        list.add(toPosition, list.removeAt(fromPosition))
        list.forEachIndexed { index, entity ->
            updateSortOrder(entity.id, index + 1)
        }
    }

}
