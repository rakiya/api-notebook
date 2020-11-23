package habanero.app.repositories

import habanero.exceptions.DuplicateException
import habanero.exceptions.HabaneroUnexpectedException
import habanero.extensions.database.tables.Section.SECTION
import habanero.extensions.database.tables.SectionTag.SECTION_TAG
import habanero.extensions.database.tables.Tag.TAG
import habanero.extensions.database.tables.records.SectionTagDetailRecord
import habanero.extensions.database.tables.records.SectionTagRecord
import habanero.extensions.database.tables.records.TagRecord
import org.jooq.DSLContext
import org.jooq.types.UInteger
import java.sql.SQLIntegrityConstraintViolationException

/**
 * セクションタグのリポジトリ
 *
 * @author Ryutaro Akiya
 */
class SectionTagRepository(tx: DSLContext) : Repository<TagRecord>(tx) {

    /**
     * 対象のセクションに付いているタグを全て取得する。
     *
     * @return List<SectionTagDetailRecord> タグ
     */
    fun getAllTagsOf(sectionId: Long): List<SectionTagDetailRecord> {
        return tx.select(
                SECTION_TAG.SECTION_ID,
                SECTION_TAG.TAG_ID,
                TAG.NAME,
                SECTION_TAG.CREATED_AT,
                SECTION_TAG.UPDATED_AT
        )
                .from(SECTION)
                .innerJoin(SECTION_TAG).using(SECTION.SECTION_ID)
                .innerJoin(TAG).using(SECTION_TAG.TAG_ID)
                .where(SECTION.SECTION_ID.eq(UInteger.valueOf(sectionId)))
                .fetchInto(SectionTagDetailRecord::class.java)
    }

    /**
     * 対象のセクションに、対象のタグを付ける。
     */
    fun createOn(sectionId: Long) {
        kotlin.runCatching {
            tx.insertInto(SECTION_TAG)
                    .set(SECTION_TAG.TAG_ID, target.tagId)
                    .set(SECTION_TAG.SECTION_ID, UInteger.valueOf(sectionId))
                    .execute()
        }.onFailure {
            when (it.cause) {
                is SQLIntegrityConstraintViolationException -> throw DuplicateException()
                else -> throw HabaneroUnexpectedException(it)
            }
        }
    }

    /**
     * 対象のセクションから、対象のタグを削除する。
     */
    fun deleteFrom(sectionId: Long) {
        tx.deleteFrom(SECTION_TAG)
                .where(SECTION_TAG.SECTION_ID.eq(UInteger.valueOf(sectionId)))
                .and(SECTION_TAG.TAG_ID.eq(target.tagId))
                .execute()
    }

    /**
     * 対象のセクションに、対象のタグが付いているか判定する。
     *
     * @return Boolean タグが付いている場合true、付いていない場合false。
     */
    fun isTaggedOn(sectionId: Long): Boolean {
        val tag: SectionTagRecord? = tx.selectFrom(SECTION_TAG)
                .where(SECTION_TAG.SECTION_ID.eq(UInteger.valueOf(sectionId)))
                .and(SECTION_TAG.TAG_ID.eq(target.tagId))
                .fetchOne()

        return tag != null
    }
}