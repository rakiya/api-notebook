package habanero.app.responses.sectionTagDetail

import java.time.LocalDateTime

data class SectionTagDetailResponse(
        val sectionId: Long,
        val tags: List<TagDetail>
) {
    data class TagDetail(
            val tagId: Long,
            val name: String,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
    )
}