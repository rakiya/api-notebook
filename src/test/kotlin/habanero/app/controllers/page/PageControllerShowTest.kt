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
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.Page
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class PageControllerShowTest : ControllerTest() {

    data class Response(
            val pageId: Long,
            val notebookId: Long,
            val title: String,
            val order: Int,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
    )

    companion object {
        private val myAccount = AccountEntity("user1")
        private val othersAccount = AccountEntity("others")

        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
                    .toList()
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(notebooks[0].id)
                    .createNext(notebooks[0].id)
                    .createNext(notebooks[0].id)
                    .createNext(notebooks[1].id)
                    .createNext(notebooks[1].id)
                    .createNext(notebooks[1].id)
                    .createNext(notebooks[2].id)
                    .createNext(notebooks[2].id)
                    .toList()
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(Notebook.NOTEBOOK).execute()
            db.deleteFrom(Page.PAGE).execute()
            db.batchInsert(notebooks.map { it.toRecord() }).execute()
            db.batchInsert(pages.map { it.toRecord() }).execute()
        }
    }

    @Test
    @DisplayName("自分が所有するページを取得できること")
    fun `You can get your page`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val myPage = pages.first { it.notebookId == myNotebook.id }

        get<Response>("/page/${myPage.pageId}", myAccount) {
            val expected = myPage
                    .toRecord()
                    .into(Response::class.java)

            validate(expected, it)
        }
    }

    @Test
    @DisplayName("他人が所有するページを取得できないこと")
    fun `You cannot get others' page`() {
        val othersNotebook = notebooks.first { it.accountId == othersAccount.id }
        val othersPage = pages.first { it.notebookId == othersNotebook.id }

        get<NoContentResponse>("/page/${othersPage.pageId}", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
        }
    }

    @Test
    @DisplayName("存在しないページを取得できないこと")
    fun `You cannot get a page not existed`() {
        get<ErrorResponse>("/page/9999", myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                    .add("pageId", listOf("ページ(ID: 9999)は存在しません。"))

            validateFailure(expected, it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK_CODE, actual.statusCode)
        Assert.assertEquals(expected, actual.body)
    }
}