package habanero.common.models

import habanero.extensions.database.tables.records.NotebookRecord
import kotlinx.coroutines.sync.Mutex
import org.jooq.types.UInteger
import java.time.LocalDateTime

data class NotebookEntity(
        val id: Long = latestId,
        val accountId: String,
        val title: String = "ノート$id",
        val order: Int,
        val createdAt: LocalDateTime = LocalDateTime.now(),
        val updatedAt: LocalDateTime = LocalDateTime.now()
) : BasicEntity<NotebookRecord> {

    companion object {
        private val mutex: Mutex = Mutex()

        private var latestId: Long = 0

        suspend fun MutableList<NotebookEntity>.createNext(ownerId: String): MutableList<NotebookEntity> {
            mutex.lock()


            val latestOrder = this
                    .filter { it.accountId == ownerId }
                    .maxByOrNull { it.order }
                    ?.order ?: 0

            if (this.isEmpty()) {
                latestId = 1
            } else {
                latestId += 1
            }

            val newNotebook = NotebookEntity(
                    latestId,
                    ownerId,
                    "ノート${latestId}",
                    latestOrder + 1
            )

            this.add(newNotebook)

            mutex.unlock()

            return this
        }
    }

    override fun toRecord(): NotebookRecord {
        val n = this
        return NotebookRecord().apply {
            this.notebookId = UInteger.valueOf(n.id)
            this.accountId = n.accountId
            this.title = n.title
            this.order = UInteger.valueOf(n.order)
            this.createdAt = n.createdAt.withNano(0)
            this.updatedAt = n.updatedAt.withNano(0)
        }
    }
}