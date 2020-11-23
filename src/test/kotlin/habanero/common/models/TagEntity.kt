package habanero.common.models

import habanero.extensions.database.tables.records.TagRecord
import kotlinx.coroutines.sync.Mutex
import org.jooq.types.UInteger
import java.time.LocalDateTime

data class TagEntity(
        val tagId: Long,
        val name: String,
        val accountId: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
) : BasicEntity<TagRecord> {

    companion object {

        private val mutex: Mutex = Mutex()

        private var latestId: Long = 0


        suspend fun MutableList<TagEntity>.createNext(account: AccountEntity): MutableList<TagEntity> {
            mutex.lock()

            val tagEntity = TagEntity(
                    ++latestId,
                    "タグ$latestId",
                    account.id,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            )

            this.add(tagEntity)

            mutex.unlock()

            return this
        }
    }

    override fun toRecord(): TagRecord {
        val t = this

        return TagRecord().apply {
            tagId = UInteger.valueOf(t.tagId)
            name = t.name
            accountId = t.accountId
            createdAt = t.createdAt.withNano(0)
            updatedAt = t.updatedAt.withNano(0)
        }
    }


}

