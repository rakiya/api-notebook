package habanero.app.responses.page

import java.time.LocalDateTime

data class PageResponse(
        val pageId: Long,
        val notebookId: Long,
        val title: String,
        val order: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
)

data class PageBatchResponse(
        val pages: List<PageResponse>
)
