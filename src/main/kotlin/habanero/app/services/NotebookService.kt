package habanero.app.services

import habanero.app.repositories.NotebookRepository
import habanero.app.repositories.Repository.Companion.of
import habanero.exceptions.ForbiddenException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.records.NotebookRecord
import habanero.extensions.jooq.transaction
import org.jooq.DSLContext
import org.jooq.types.UInteger
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.profile.CommonProfile

/**
 * ノートに関する処理
 *
 * @author Ryutaro Akiya
 */
class NotebookService : KoinComponent {

    companion object : KoinComponent {

        private val db: DSLContext by inject()

        /**
         * ユーザの操作によって、ノートを取得する。
         *
         * @receiver CommonProfile 操作するユーザ
         * @param notebookId Long 取得するノートのID
         * @return NotebookRecord? ノート
         *
         * @throws ForbiddenException ノートの所有者がユーザでない。
         */
        fun CommonProfile.getsNotebook(notebookId: Long): NotebookRecord? {
            var notebook: NotebookRecord? = null

            db.transaction { tx: DSLContext ->
                val repo = NotebookRepository(tx)

                notebook = repo.get(notebookId)
                        // ノートが存在する場合、所有者がユーザか確認
                        .also { if (it != null && !repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }

            }

            return notebook
        }

        /**
         * ユーザの操作によってノートを作成する。
         *
         * @receiver CommonProfile 操作するユーザ
         * @param title String 作成するノートのタイトル
         * @return NotebookRecord 作成したノート
         */
        fun CommonProfile.createsNotebook(title: String): NotebookRecord {
            val user = this

            lateinit var newNotebook: NotebookRecord

            db.transaction { tx: DSLContext ->
                val record = NotebookRecord().apply {
                    this.accountId = user.id
                    this.title = title
                }

                newNotebook = NotebookRepository(tx).of(record).create()
            }

            return newNotebook
        }

        /**
         * ユーザの操作によってノートを更新する。
         *
         * @receiver CommonProfile 操作するユーザ
         * @param updatedNotebookId Long 更新するノートのID
         * @param title String 更新後のタイトル
         * @param order Int 更新後の順序
         * @return NotebookRecord 更新後のノート
         *
         * @throws NotFoundException 更新対象のノートが存在しない。
         * @throws ForbiddenException 更新対象のノートがユーザのものでない。
         */
        fun CommonProfile.updatesNotebook(updatedNotebookId: Long, title: String, order: Int): NotebookRecord {
            // 更新後のノート
            lateinit var updatedNotebook: NotebookRecord

            db.transaction { tx: DSLContext ->
                val repo = NotebookRepository(tx)

                // 更新後の状態を定義
                val record = NotebookRecord().apply {
                    this.title = title
                    this.order = UInteger.valueOf(order)
                }

                // 更新対象を取得して更新
                updatedNotebook = repo.get(updatedNotebookId, forUpdate = true)
                        // ノートが存在するか確認
                        .let { it ?: throw NotFoundException(Notebook::class, updatedNotebookId) }
                        // ノートの所有者がユーザか確認
                        .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
                        // 更新
                        .let { repo.of(it).update(record) }
            }

            return updatedNotebook
        }

        /**
         * ユーザの操作によってノートを削除する。
         *
         * @receiver String ユーザ
         * @param notebookId Long 削除するノートのID
         * @throws NotFoundException 対象のノートが見つからない。
         * @throws ForbiddenException 対象のノートがユーザの持ち物でない。
         */
        fun CommonProfile.deletesNotebook(notebookId: Long) {
            db.transaction { tx: DSLContext ->
                val repo = NotebookRepository(tx)

                repo.get(notebookId, forUpdate = true)
                        // ノートが存在するか確認
                        .let { it ?: throw NotFoundException(Notebook::class, notebookId) }
                        // ノートの持ち主がユーザか確認
                        .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
                        // ノートを削除
                        .also { repo.of(it).delete() }
            }
        }
    }

    /**
     * 指定のユーザが所有する全てのノート取得する。
     *
     * @param user String ノートを取得されるユーザ
     * @return List<NotebookRecord> ユーザが所有する全ノート
     */
    fun getAllOwnedBy(user: CommonProfile): List<NotebookRecord> {
        return NotebookRepository(db).getOwnedBy(user.id)
    }

}
