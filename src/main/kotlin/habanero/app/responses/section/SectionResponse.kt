package habanero.app.responses.section

import java.time.LocalDateTime

data class SectionResponse(
        val sectionId: Long,
        val pageId: Long,
        val content: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
)

