package habanero.common.models

import habanero.extensions.database.tables.records.SectionRecord
import kotlinx.coroutines.sync.Mutex
import org.jooq.types.UInteger
import java.time.LocalDateTime

data class SectionEntity(
        val sectionId: Long = 1,
        val pageId: Long = 1,
        val content: String = "コンテンツ${sectionId}",
        val isTrashed: Boolean = false,
        val createdAt: LocalDateTime = LocalDateTime.now(),
        val updatedAt: LocalDateTime = LocalDateTime.now()
) : BasicEntity<SectionRecord> {

    companion object {

        private val mutex: Mutex = Mutex()

        private var latestId: Long = 0

        suspend fun MutableList<SectionEntity>.createNext(pageId: Long): MutableList<SectionEntity> {
            mutex.lock()

            if (this.isEmpty()) {
                latestId = 1
            } else {
                latestId += 1
            }

            val sectionEntity = SectionEntity(
                    latestId,
                    pageId,
                    "コンテンツ$latestId",
            )

            this.add(sectionEntity)

            mutex.unlock()

            return this
        }

        suspend fun MutableList<SectionEntity>.trashed(): MutableList<SectionEntity> {
            mutex.lock()

            if (this.isEmpty()) throw NullPointerException()

            val last = this.removeLast()
            this.add(last.copy(isTrashed = true))

            mutex.unlock()

            return this
        }

    }

    override fun toRecord(): SectionRecord {
        val s = this
        return SectionRecord().apply {
            sectionId = UInteger.valueOf(s.sectionId)
            pageId = UInteger.valueOf(s.pageId)
            content = s.content
            isTrashed = if (s.isTrashed) 1 else 0
            createdAt = s.createdAt.withNano(0)
            updatedAt = s.updatedAt.withNano(0)
        }
    }
}