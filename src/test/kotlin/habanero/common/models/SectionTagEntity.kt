package habanero.common.models

import habanero.extensions.database.tables.records.SectionTagRecord
import org.jooq.types.UInteger
import java.time.LocalDateTime

data class SectionTagEntity(
        val sectionId: Long,
        val tagId: Long,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
) : BasicEntity<SectionTagRecord> {

    companion object {

        fun MutableList<SectionTagEntity>.createNext(section: SectionEntity, tag: TagEntity): MutableList<SectionTagEntity> {
            val sectionTag = SectionTagEntity(
                    section.sectionId,
                    tag.tagId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            )

            this.add(sectionTag)

            return this
        }

        fun List<SectionTagEntity>.of(sectionId: Long): List<SectionTagEntity> {
            return this.filter { it.sectionId == sectionId }
        }
    }

    override fun toRecord(): SectionTagRecord {
        val st = this

        return SectionTagRecord().apply {
            sectionId = UInteger.valueOf(st.sectionId)
            tagId = UInteger.valueOf(st.tagId)
            createdAt = st.createdAt.withNano(0)
            updatedAt = st.updatedAt.withNano(0)
        }
    }
}