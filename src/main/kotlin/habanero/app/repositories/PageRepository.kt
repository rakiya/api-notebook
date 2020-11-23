package habanero.app.repositories

import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.Page.PAGE
import habanero.extensions.database.tables.records.NotebookRecord
import habanero.extensions.database.tables.records.PageRecord
import habanero.extensions.jooq.forUpdateIf
import org.jooq.DSLContext
import org.jooq.types.UInteger

/**
 * ページのリポジトリ
 *
 * @author Ryutaro Akiya
 */
class PageRepository(tx: DSLContext) : Repository<PageRecord>(tx) {

    // 対象のノート
    private lateinit var targetNotebook: NotebookRecord

    /**
     * 対象のノートを設定する。
     * @param target NotebookRecord 対象のノート
     * @return PageRepository リポジトリ
     */
    fun of(target: NotebookRecord): PageRepository {
        targetNotebook = target
        return this
    }

    /**
     * 指定のノートの全てのページを取得する。
     *
     * @receiver NotebookRecord 指定のノート
     * @return List<PageRecord> ノート内の全ページ
     */
    fun getAll(): List<PageRecord> {
        return tx.selectFrom(PAGE)
                .where(PAGE.NOTEBOOK_ID.eq(targetNotebook.notebookId))
                .fetchInto(PAGE)
    }

    /**
     * 特定のページの情報を取得する。
     *
     * @param pageId String 取得するページのID
     * @return PageRecord? 取得したページ。存在しない場合はnull。
     */
    fun get(pageId: Long, forUpdate: Boolean = false): PageRecord? {
        return tx.selectFrom(PAGE)
                .where(PAGE.PAGE_ID.eq(UInteger.valueOf(pageId)))
                .forUpdateIf(forUpdate)
                .fetchOne()
                ?.into(PAGE)
    }

    /**
     * 指定のノートにページを追加する。
     *
     * @return PageRecord 追加したページ
     */
    fun create(): PageRecord {
        tx.shiftOrderAfter(target.order, targetNotebook.notebookId)

        val pageId = tx.insertInto(PAGE)
                .set(PAGE.NOTEBOOK_ID, targetNotebook.notebookId)
                .set(PAGE.TITLE, target.title)
                .set(PAGE.ORDER, target.order)
                .returningResult(PAGE.PAGE_ID)
                .fetchOne()
                .component1()

        return tx.selectFrom(PAGE)
                .where(PAGE.PAGE_ID.eq(pageId))
                .fetchOneInto(PAGE)
    }

    /**
     * ページの情報を更新する。
     *
     * @param record PageRecord 更新後の内容
     * @return PageRecord 更新後のページ
     */
    fun update(record: PageRecord): PageRecord {
        tx.shiftOrderAfter(record.order, record.notebookId)

        tx.update(PAGE)
                .set(PAGE.NOTEBOOK_ID, record.notebookId)
                .set(PAGE.TITLE, record.title)
                .set(PAGE.ORDER, record.order)
                .where(PAGE.PAGE_ID.eq(target.pageId))
                .execute()

        return tx.selectFrom(PAGE)
                .where(PAGE.PAGE_ID.eq(target.pageId))
                .fetchOneInto(PAGE)
    }

    fun delete() {
        tx.deleteFrom(PAGE)
                .where(PAGE.PAGE_ID.eq(target.pageId))
                .execute()
    }

    /**
     * ユーザが所有しているページかを判定する。
     *
     * @param accountId String 所有者
     * @return Boolean 所有している場合true、所有していない場合false
     */
    fun isOwnedBy(accountId: String): Boolean {
        val actualOwnerId: String? = tx.select(Notebook.NOTEBOOK.ACCOUNT_ID)
                .from(Notebook.NOTEBOOK)
                .innerJoin(PAGE).using(PAGE.NOTEBOOK_ID)
                .where(PAGE.PAGE_ID.eq(target.pageId))
                .fetchOneInto(String::class.java)

        return actualOwnerId != null && actualOwnerId == accountId
    }

    /**
     * ページの順序を変更する際に、orderInclusive以降のページの順序を1だけ増やす。
     *
     * @receiver DSLContext トランザクション
     * @param orderInclusive UInteger シフトしたい順序(含む)
     * @param notebookId UInteger シフトしたいページを含むノートのID
     */
    private fun DSLContext.shiftOrderAfter(orderInclusive: UInteger, notebookId: UInteger) {
        this.update(PAGE)
                .set(PAGE.ORDER, PAGE.ORDER.add(1))
                .where(PAGE.NOTEBOOK_ID.eq(notebookId))
                .and(PAGE.ORDER.ge(orderInclusive))
                .orderBy(PAGE.ORDER.desc())
                .execute()
    }
}