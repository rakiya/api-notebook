package habanero.extensions.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.LocalDateTime

/**
 * Gsonパーサを作成する
 */
object TestGsonBuilder {
    fun build(): Gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, TestLocalDateTimeAdapter())
            .create()
}