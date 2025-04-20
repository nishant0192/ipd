package com.google.mediapipe.examples.poselandmarker.recommend

import com.google.mediapipe.examples.poselandmarker.data.RatingEntity
import com.google.mediapipe.examples.poselandmarker.data.RatingDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class ItemBasedRecommender(private val dao: RatingDao) {
  suspend fun recommend(k: Int = 3): List<String> = withContext(Dispatchers.Default) {
    val ratings = dao.allRatings()
    if (ratings.size<2) return@withContext emptyList()
    val byW = ratings.groupBy{ it.workoutId }
                 .mapValues{ it.value.map{ r-> r.rating.toFloat() } }
    val last = ratings.last().workoutId

    fun cosine(a: List<Float>, b: List<Float>): Float {
      val dot = a.zip(b).sumOf{(x,y)-> x*y}
      val magA = sqrt(a.sumOf{x->x*x}); val magB = sqrt(b.sumOf{y->y*y})
      return if (magA>0&&magB>0) dot/(magA*magB) else 0f
    }

    byW.filterKeys{ it!=last }
       .mapValues{ cosine(byW[last]!!, it.value) }
       .entries.sortedByDescending{ it.value }
       .take(k)
       .map{ it.key }
  }
}
