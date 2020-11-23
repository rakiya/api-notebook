package habanero.app.services

import habanero.app.repositories.NotebookRepository
import habanero.app.repositories.PageRepository
import habanero.app.repositories.Repository.Companion.of
import habanero.exceptions.ForbiddenException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.Page
import habanero.extensions.database.tables.records.NotebookRecord
import habanero.extensions.database.tables.records.PageRecord
import habanero.extensions.jooq.transaction
import org.jooq.DSLContext
import org.jooq.types.UInteger
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.profile.CommonProfile

/**
 * ページに関する処理
 *
 * @author Ryutaro Akiya
 */
class PageService : KoinComponent {

    companion object : KoinComponent {

        data class AddPageInstruction(val user: CommonProfile, val page: PageRecord)

        data class UpdatePageInstruction(val user: CommonProfile, val page: PageRecord)

        private val db: DSLContext by inject()

        /**
         * 指定のノートの全てのページを取得する。
         *
         * @receiver NotebookRecord 指定のノート
         * @return List<PageRecord> ノート内の全ページ
         */
        fun NotebookRecord.getAllPages(): List<PageRecord> {
            return PageRepository(db).of(this).getAll()
        }

        /**
         * 指定されたページを取得する。
         *
         * @receiver CommonProfile 取得するユーザ
         * @param pageId Long 取得するページのID
         * @return PageRecord? ページ
         */
        fun CommonProfile.getsPage(pageId: Long): PageRecord? {
            var page: PageRecord? = null

            db.transaction { tx: DSLContext ->
                val repo = PageRepository(tx)

                page = repo.get(pageId)
                        .also {
                            // ページがユーザのものか確認
                            if (it != null && !repo.of(it).isOwnedBy(this.id)) throw ForbiddenException()
                        }
            }

            return page
        }

        /**
         * ノートを追加する命令を作成する。
         *
         * @receiver CommonProfile 操作するユーザ
         * @param title String 作成するページのタイトル
         * @param order Int 作成するページの順序
         * @return AddPageInstruction ページ作成命令
         */
        fun CommonProfile.addsPage(title: String, order: Int): AddPageInstruction {
            val record = PageRecord().apply {
                this.title = title
                this.order = UInteger.valueOf(order)
            }

            return AddPageInstruction(this, record)
        }

        /**
         * ページを作成する。
         *
         * @receiver AddPageInstruction ページ作成命令
         * @param notebookId Long 追加先のノート
         * @return PageRecord 作成したページ
         */
        fun AddPageInstruction.toNotebook(notebookId: Long): PageRecord {
            lateinit var newPage: PageRecord

            db.transaction { tx: DSLContext ->
                val notebookRepo = NotebookRepository(tx)

                // 追加先のノートを取得
                val notebook = notebookRepo.get(notebookId, forUpdate = true)
                        // ノートが存在するか確認
                        .let { it ?: throw NotFoundException(Notebook::class, notebookId) }
                        // ノートの所有者がユーザか確認
                        .also { if (!notebookRepo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                // ノートにページを追加
                newPage = PageRepository(tx).of(notebook).of(this.page).create()
            }

            return newPage
        }

        /**
         * ページを更新する命令を作成する。
         *
         * @receiver CommonProfile 操作するユーザ
         * @param notebookId Long 更新後のノートID
         * @param title String 更新後のタイトル
         * @param order Int 更新後の順序
         * @return UpdatePageInstruction ページ更新命令
         */
        fun CommonProfile.changes(notebookId: Long, title: String, order: Int): UpdatePageInstruction {
            val page = PageRecord().apply {
                this.notebookId = UInteger.valueOf(notebookId)
                this.title = title
                this.order = UInteger.valueOf(order)
            }

            return UpdatePageInstruction(this, page)
        }

        /**
         * ページを更新する。
         *
         * @receiver UpdatePageInstruction ページ更新命令
         * @param pageId Long 更新するページのID
         * @return PageRecord 更新後のページ
         */
        fun UpdatePageInstruction.ofPage(pageId: Long): PageRecord {
            lateinit var updatedPage: PageRecord

            db.transaction { tx: DSLContext ->
                val notebookRepo = NotebookRepository(tx)
                val repo = PageRepository(tx)

                notebookRepo.get(this.page.notebookId.toLong(), forUpdate = true)
                        // ノートが存在するか確認
                        .let { it ?: throw NotFoundException(Notebook::class, this.page.notebookId.toLong()) }

                val target = repo.get(pageId, forUpdate = true)
                        // ページが存在するか確認
                        .let { it ?: throw NotFoundException(Page::class, pageId) }
                        // ページの所有者がユーザか確認
                        .also { if (!repo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                updatedPage = repo.of(target).update(this.page)
            }

            return updatedPage
        }

        /**
         * ページを削除する。
         *
         * @receiver CommonProfile ページの所有者
         * @param pageId Long 削除するページのID
         */
        fun CommonProfile.deletesPage(pageId: Long) {
            db.transaction { tx: DSLContext ->
                val repo = PageRepository(tx)

                val page = repo.get(pageId, forUpdate = true)
                        // ページが存在するか確認
                        .let { it ?: throw NotFoundException(Page::class, pageId) }
                        // ページの所有者がユーザか確認
                        .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }

                repo.of(page).delete()
            }
        }

    }

}

