package habanero.app.repositories

import habanero.app.services.SectionService
import habanero.extensions.database.Tables
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.Page
import habanero.extensions.database.tables.Section.SECTION
import habanero.extensions.database.tables.records.PageRecord
import habanero.extensions.database.tables.records.SectionRecord
import habanero.extensions.jooq.forUpdateIf
import org.jooq.DSLContext
import org.jooq.types.UInteger

/**
 * セクションのリポジトリ
 *
 * @author Ryutaro Akiya
 */
class SectionRepository(tx: DSLContext) : Repository<SectionRecord>(tx) {

    // 対象のページ
    private lateinit var targetPage: PageRecord

    /**
     * 対象のページを設定する。
     *
     * @param target PageRecord 対象のページ
     * @return SectionRepository リポジトリ
     */
    fun of(target: PageRecord): SectionRepository {
        this.targetPage = target
        return this
    }

    /**
     * 指定のページ内の全てのセクションを取得する。
     *
     * @return List<SectionRecord> ページに含まれる全てのセクション
     */
    fun getAll(): List<SectionRecord> {
        return tx.selectFrom(SECTION)
                .where(SECTION.PAGE_ID.eq(targetPage.pageId))
                .and(SECTION.IS_TRASHED.eq(0))
                .fetchInto(SECTION)
    }

    /**
     * アカウントIDがownerIdのユーザが所有する、ゴミ箱内のセクションを取得する。
     *
     * @param ownerId String 所有者
     * @return List<SectionRecord> ゴミ箱内のセクション
     */
    fun getAllTrashedOwnedBy(ownerId: String): List<SectionRecord> {
        return tx.select()
                .from(SECTION)
                .innerJoin(Page.PAGE).using(Page.PAGE.PAGE_ID)
                .innerJoin(Tables.NOTEBOOK).using(Tables.NOTEBOOK.NOTEBOOK_ID)
                .where(Tables.NOTEBOOK.ACCOUNT_ID.eq(ownerId))
                .and(SECTION.IS_TRASHED.eq(1))
                .fetchInto(SECTION)
    }

    /**
     * IDがsectionIdのセクションを取得する。
     *
     * @param sectionId Long セクションID
     * @return SectionRecord? セクション
     */
    fun get(sectionId: Long, forUpdate: Boolean = false): SectionRecord? {
        return tx.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(UInteger.valueOf(sectionId)))
                .forUpdateIf(forUpdate)
                .fetchOneInto(SECTION)
    }

    /**
     * 指定されたページにセクションを追加する。
     *
     * @return SectionRecord 追加されたセクション
     */
    fun create(): SectionRecord {
        val sectionId = tx.insertInto(SECTION)
                .set(target)
                .set(SECTION.PAGE_ID, targetPage.pageId)
                .returningResult(SECTION.SECTION_ID)
                .fetchOne()
                .component1()

        return tx.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(sectionId))
                .fetchOneInto(SECTION)
    }

    /**
     * 指定されたセクションを更新する。
     *
     * @param record SectionRecord 更新する内容
     * @return SectionRecord
     */
    fun update(record: SectionRecord): SectionRecord {
        tx.update(SECTION)
                .set(record)
                .where(SECTION.SECTION_ID.eq(target.sectionId))
                .execute()

        return tx.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(target.sectionId))
                .fetchOneInto(SECTION)
    }

    /**
     * セクションを削除する。
     */
    fun delete() {
        tx.deleteFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(target.sectionId))
                .execute()
    }

    /**
     * IDがaccountIdのアカウントが指定のセクションを所有しているか判定する。
     *
     * @param accountId String 所有者
     * @return Boolean 所有している場合はtrue、していない場合はfalse。
     */
    fun isOwnedBy(accountId: String): Boolean {
        val actualOwnerId: String? = SectionService.db.select(Notebook.NOTEBOOK.ACCOUNT_ID)
                .from(SECTION)
                .innerJoin(Page.PAGE).using(Page.PAGE.PAGE_ID)
                .innerJoin(Notebook.NOTEBOOK).using(Notebook.NOTEBOOK.NOTEBOOK_ID)
                .where(SECTION.SECTION_ID.eq(target.sectionId))
                .fetchOneInto(String::class.java)

        return actualOwnerId != null && actualOwnerId == accountId
    }
}