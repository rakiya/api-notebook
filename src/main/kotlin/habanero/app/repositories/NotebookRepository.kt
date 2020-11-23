package habanero.app.repositories

import habanero.exceptions.DuplicateException
import habanero.exceptions.HabaneroUnexpectedException
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.records.NotebookRecord
import habanero.extensions.jooq.forUpdateIf
import org.jooq.DSLContext
import org.jooq.types.UInteger
import java.sql.SQLIntegrityConstraintViolationException

/**
 * ノートのリポジトリ
 *
 * @author Ryutaro Akiya
 */
class NotebookRepository(tx: DSLContext) : Repository<NotebookRecord>(tx) {

    /**
     * 指定のユーザが所有する全てのノート取得する。
     *
     * @param accountId String ノートを取得されるユーザ
     * @return List<NotebookRecord> ユーザが所有する全ノート
     */
    fun getOwnedBy(accountId: String): List<NotebookRecord> {
        return tx.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.ACCOUNT_ID.eq(accountId))
                .orderBy(NOTEBOOK.ORDER.asc())
                .fetchInto(NOTEBOOK)
    }

    /**
     * 指定のノートを取得する。
     *
     * @param notebookId Long 取得したいノートのID
     * @return NotebookRecord? 取得したノート。存在しない場合null。
     */
    fun get(notebookId: Long, forUpdate: Boolean = false): NotebookRecord? {
        return tx.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(notebookId)))
                .forUpdateIf(forUpdate)
                .fetchOneInto(NOTEBOOK)
    }

    /**
     * 新しいノートを作成する。
     *
     * @return NotebookRecord 作成したノート
     */
    fun create(): NotebookRecord {
        lateinit var newNotebook: NotebookRecord

        val lastOrder = tx.select(NOTEBOOK.ORDER)
                .from(NOTEBOOK)
                .where(NOTEBOOK.ACCOUNT_ID.eq(target.accountId))
                .orderBy(NOTEBOOK.ORDER.desc())
                .limit(1)
                .fetchOneInto(UInteger::class.java)

        kotlin.runCatching {
            newNotebook = tx.insertInto(NOTEBOOK)
                    .set(NOTEBOOK.ACCOUNT_ID, target.accountId)
                    .set(NOTEBOOK.TITLE, target.title)
                    .set(NOTEBOOK.ORDER, lastOrder.add(1))
                    .returning()
                    .fetchOne()
                    .into(NOTEBOOK)
        }.onFailure {
            when (it.cause) {
                is SQLIntegrityConstraintViolationException -> throw DuplicateException()
                else -> throw HabaneroUnexpectedException(it)
            }
        }

        return newNotebook
    }

    /**
     * ノートの情報を更新する。
     *
     * @param record NotebookRecord 更新内容
     * @return NotebookRecord 更新後のノート
     */
    fun update(record: NotebookRecord): NotebookRecord {
        shiftOrderAfter(record.order)

        kotlin.runCatching {
            tx.update(NOTEBOOK)
                    .set(NOTEBOOK.TITLE, record.title)
                    .set(NOTEBOOK.ORDER, record.order)
                    .where(NOTEBOOK.NOTEBOOK_ID.eq(target.notebookId))
                    .execute()
        }.onFailure {
            when (it.cause) {
                is SQLIntegrityConstraintViolationException -> throw DuplicateException()
                else -> throw HabaneroUnexpectedException(it)
            }
        }

        return tx.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(target.notebookId))
                .fetchOneInto(NOTEBOOK)
    }

    /**
     * 指定されたノートを削除する。
     */
    fun delete() {
        tx.deleteFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(target.notebookId))
                .execute()
    }

    /**
     * ノートが指定のユーザに所有されているかを確認する。
     *
     * @param accountId String 所有者
     * @return Boolean 所有者が指定のユーザの場合true、所有者がその他のユーザの場合false
     */
    fun isOwnedBy(accountId: String): Boolean {
        val actualOwnerId: String? = tx.select(NOTEBOOK.ACCOUNT_ID)
                .from(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(target.notebookId))
                .fetchOneInto(String::class.java)

        return actualOwnerId != null && actualOwnerId == accountId
    }

    /**
     * ノートの順序を変更する際に、orderInclusive以降のページの順序を1だけ増やす。
     *
     * @param orderInclusive UInteger シフトしたい順序(含む)
     */
    private fun shiftOrderAfter(orderInclusive: UInteger) {
        tx.update(NOTEBOOK)
                .set(NOTEBOOK.ORDER, NOTEBOOK.ORDER.add(1))
                .where(NOTEBOOK.ACCOUNT_ID.eq(target.accountId))
                .and(NOTEBOOK.ORDER.ge(orderInclusive))
                .orderBy(NOTEBOOK.ORDER.desc())
                .execute()
    }
}