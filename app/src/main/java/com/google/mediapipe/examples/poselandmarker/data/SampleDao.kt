package com.google.mediapipe.examples.poselandmarker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SampleDao {
  @Insert suspend fun insert(sample: SampleEntity)
  @Query("SELECT * FROM samples ORDER BY timestamp DESC LIMIT :limit")
  suspend fun getRecent(limit: Int): List<SampleEntity>
}
