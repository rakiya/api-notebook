package habanero.app.controllers.section

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.common.models.NotebookEntity
import habanero.common.models.NotebookEntity.Companion.createNext
import habanero.common.models.PageEntity
import habanero.common.models.PageEntity.Companion.createNext
import habanero.common.models.SectionEntity
import habanero.common.models.SectionEntity.Companion.createNext
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.Page.PAGE
import habanero.extensions.database.tables.Section.SECTION
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@JoobyTest(Application::class)
class SectionControllerDeleteTest : ControllerTest() {

    companion object {
        private val myAccount = AccountEntity("mine")
        private val othersAccount = AccountEntity("others")

        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
                    .toList()
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(1)
                    .createNext(2)
        }

        private val sections = runBlocking {
            mutableListOf<SectionEntity>()
                    .createNext(1)
                    .createNext(1)
                    .createNext(2)
                    .createNext(2)
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(NOTEBOOK).execute()
            db.deleteFrom(PAGE).execute()
            db.deleteFrom(SECTION).execute()
            db.batchInsert(notebooks.map { it.toRecord() }).execute()
            db.batchInsert(pages.map { it.toRecord() }).execute()
        }
    }

    @BeforeEach
    fun beforeEach() {
        db.deleteFrom(SECTION).execute()
        db.batchInsert(sections.map { it.toRecord() }).execute()
    }

    @Test
    @DisplayName("自分が所有するセクションを取得できること")
    fun `You can delete your section`() {
        val target = getSectionOwnedBy(myAccount)

        validateRecordCount(NOTEBOOK to 0, PAGE to 0, SECTION to -1) {
            delete<NoContentResponse>("/section/${target.sectionId}", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.NO_CONTENT), it)
            }
        }
    }

    @Test
    @DisplayName("他人が所有するセクションを取得できないこと")
    fun `You cannot delete the others' section`() {
        val target = getSectionOwnedBy(othersAccount)

        validateRecordCount(NOTEBOOK to 0, PAGE to 0, SECTION to 0) {
            delete<NoContentResponse>("/section/${target.sectionId}", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val target = getSectionOwnedBy(myAccount)

        validateRecordCount(NOTEBOOK to 0, PAGE to 0, SECTION to 0) {
            delete<NoContentResponse>("/section/${target.sectionId}") {
                validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
            }
        }
    }

    @Test
    @DisplayName("存在しないセクションを削除できないこと")
    fun `You cannot delete a section not existed`() {
        validateRecordCount(NOTEBOOK to 0, PAGE to 0, SECTION to 0) {
            delete<NoContentResponse>("/section/9999", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.NOT_FOUND), it)
            }
        }
    }

    private fun getSectionOwnedBy(account: AccountEntity, offset: Int = 0): SectionEntity {
        val notebookIds = notebooks
                .filter { it.accountId == account.id }
                .map { it.id }
        val pageIds = pages
                .filter { notebookIds.contains(it.notebookId) }
                .map { it.pageId }

        return sections.filter { pageIds.contains(it.pageId) }[offset]
    }
}