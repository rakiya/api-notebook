package habanero.extensions.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * LocalDateTimeをJSONから変換するためのパーサ
 */
internal class TestLocalDateTimeAdapter : JsonDeserializer<LocalDateTime?> {

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")!!
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
        return LocalDateTime.parse(json.asJsonPrimitive.asString, formatter)
    }
}
