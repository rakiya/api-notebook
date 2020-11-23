package habanero.extensions.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.LocalDateTime

/**
 * 独自のGsonパーサを作成する
 *
 * @author Ryutaro Akiya
 */
object GsonBuilder {
    fun build(): Gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
}