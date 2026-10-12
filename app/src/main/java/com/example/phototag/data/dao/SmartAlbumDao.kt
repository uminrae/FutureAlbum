package com.example.phototag.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.phototag.data.entity.SmartAlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartAlbumDao {

    @Query("SELECT * FROM smart_albums ORDER BY createdAt ASC")
    fun getAllSmartAlbumsFlow(): Flow<List<SmartAlbumEntity>>

    @Query("SELECT * FROM smart_albums ORDER BY createdAt ASC")
    suspend fun getAllSmartAlbumsList(): List<SmartAlbumEntity>

    @Query("SELECT * FROM smart_albums WHERE id = :id")
    suspend fun getSmartAlbumById(id: Long): SmartAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartAlbum(album: SmartAlbumEntity): Long

    @Update
    suspend fun updateSmartAlbum(album: SmartAlbumEntity)

    @Delete
    suspend fun deleteSmartAlbum(album: SmartAlbumEntity)

    @Query("DELETE FROM smart_albums WHERE id = :id")
    suspend fun deleteSmartAlbumById(id: Long)
}
