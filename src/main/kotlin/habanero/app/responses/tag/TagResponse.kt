package habanero.app.responses.tag

import java.time.LocalDateTime

data class TagResponse(
        val tagId: Long,
        val name: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
)