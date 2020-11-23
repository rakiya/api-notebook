package habanero.app.controllers.page

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.common.models.NotebookEntity
import habanero.common.models.NotebookEntity.Companion.createNext
import habanero.common.models.PageEntity
import habanero.common.models.PageEntity.Companion.createNext
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.Page.PAGE
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@JoobyTest(Application::class)
class PageControllerDeleteTest : ControllerTest() {

    companion object {
        private val myAccount = AccountEntity("mine")
        private val othersAccount = AccountEntity("others")

        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(1)
                    .createNext(1)
                    .createNext(2)
                    .createNext(3)
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(NOTEBOOK).execute()
            db.batchInsert(notebooks.map { it.toRecord() }).execute()
        }
    }

    @BeforeEach
    fun beforeEach() {
        db.deleteFrom(PAGE).execute()
        db.batchInsert(pages.map { it.toRecord() }).execute()
    }

    @Test
    @DisplayName("自分の所有するページを削除できること")
    fun `You can delete your page`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val target = pages.first { it.notebookId == myNotebook.id }

        validateRecordCount(-1, PAGE) {
            delete<NoContentResponse>("/page/${target.pageId}", myAccount) {
                validate(it)
            }
        }
    }

    @Test
    @DisplayName("他人の所有するページを削除できないこと")
    fun `You cannot delete an others' page`() {
        val othersNotebook = notebooks.first { it.accountId == othersAccount.id }
        val target = pages.first { it.notebookId == othersNotebook.id }

        validateRecordCount(0, PAGE) {
            delete<NoContentResponse>("/page/${target.pageId}", myAccount) {
                Assert.assertEquals(StatusCode.FORBIDDEN.value(), it.statusCode)
            }
        }
    }

    @Test
    @DisplayName("ログインが必要であること")
    fun `You cannot delete a page without logging in`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val target = pages.first { it.notebookId == myNotebook.id }

        validateRecordCount(0, PAGE) {
            delete<NoContentResponse>("/page/${target.pageId}") {
                Assert.assertEquals(StatusCode.UNAUTHORIZED.value(), it.statusCode)
            }
        }
    }

    @Test
    @DisplayName("存在しないページを削除できないこと")
    fun `You cannot delete a page not existed`() {

        validateRecordCount(0, PAGE) {
            delete<ErrorResponse>("/page/9999", myAccount) {
                val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                        .add("pageId", listOf("ページ(ID: 9999)は存在しません。"))

                validateFailure(expected, it)
            }
        }
    }

    private fun validate(actual: ResponseEntity<NoContentResponse>) {
        Assert.assertEquals(StatusCode.NO_CONTENT.value(), actual.statusCode)
    }
}