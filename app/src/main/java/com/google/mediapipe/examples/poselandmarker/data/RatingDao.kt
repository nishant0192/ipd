package com.google.mediapipe.examples.poselandmarker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RatingDao {
  @Insert suspend fun insert(r: RatingEntity)
  @Query("SELECT * FROM ratings")
  suspend fun allRatings(): List<RatingEntity>
}
