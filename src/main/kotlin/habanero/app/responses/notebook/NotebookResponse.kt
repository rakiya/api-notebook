package habanero.app.responses.notebook

import java.time.LocalDateTime

data class NotebookResponse(
        val notebookId: Long,
        val title: String,
        val order: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
)

data class NotebookBatchResponse(
        val notebooks: List<NotebookResponse>
)
