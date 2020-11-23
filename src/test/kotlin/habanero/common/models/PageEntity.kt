package habanero.common.models

import habanero.extensions.database.tables.records.PageRecord
import kotlinx.coroutines.sync.Mutex
import org.jooq.types.UInteger
import java.time.LocalDateTime

class PageEntity(
        val pageId: Long = 1,
        val notebookId: Long = 1,
        val title: String = "ページ$pageId",
        val order: Int = 1,
        val createdAt: LocalDateTime = LocalDateTime.now(),
        val updatedAt: LocalDateTime = LocalDateTime.now()

) : BasicEntity<PageRecord> {

    companion object {

        private val mutex: Mutex = Mutex()

        private var latestId: Long = 0

        suspend fun MutableList<PageEntity>.createNext(notebookId: Long): MutableList<PageEntity> {
            mutex.lock()

            val latestOrder = this
                    .filter { it.notebookId == notebookId }
                    .maxByOrNull { it.order }
                    ?.order ?: 0

            if (this.isEmpty()) {
                latestId = 1
            } else {
                latestId += 1
            }

            val pageEntity = PageEntity(
                    latestId,
                    notebookId,
                    "ページ$latestId",
                    latestOrder + 1
            )

            this.add(pageEntity)

            mutex.unlock()

            return this
        }

    }

    override fun toRecord(): PageRecord {
        val p = this
        return PageRecord().apply {
            pageId = UInteger.valueOf(p.pageId)
            notebookId = UInteger.valueOf(p.notebookId)
            title = p.title
            order = UInteger.valueOf(p.order)
            createdAt = p.createdAt.withNano(0)
            updatedAt = p.updatedAt.withNano(0)
        }
    }
}