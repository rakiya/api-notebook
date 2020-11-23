package habanero.app.controllers.page

import habanero.app.Application
import habanero.app.controllers.ControllerTest
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class PageControllerIndexTest : ControllerTest() {

    data class Response(
            val pages: List<ResponseInner>
    ) {
        data class ResponseInner(
                val pageId: Long,
                val notebookId: Long,
                val title: String,
                val order: Int,
                val createdAt: LocalDateTime,
                val updatedAt: LocalDateTime
        )
    }

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
            db.deleteFrom(NOTEBOOK).execute()
            db.deleteFrom(PAGE).execute()
            db.batchInsert(notebooks.map { it.toRecord() }).execute()
            db.batchInsert(pages.map { it.toRecord() }).execute()
        }
    }

    @Test
    @DisplayName("自分が所有するページを取得できること")
    fun `You can get your pages`() {
        val targetNotebook = notebooks[0]

        get<Response>("/notebook/${targetNotebook.id}/pages", myAccount) { res ->
            val expected = pages
                    .filter { it.notebookId == targetNotebook.id }
                    .map { it.toRecord().into(Response.ResponseInner::class.java) }
                    .let { Response(it) }

            validate(expected, res)
        }
    }

    @Test
    @DisplayName("他人の所有するページを取得できないこと")
    fun `You cannot get others' pages`() {
        val targetNotebook = notebooks[0]

        get<NoContentResponse>("/notebook/${targetNotebook.id}/pages", othersAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
        }

    }

    @Test
    @DisplayName("ログインしていること")
    fun `You should have logged in`() {
        val targetNotebook = notebooks[0]

        get<NoContentResponse>("/notebook/${targetNotebook.id}/pages") {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)

        val actualBody = actual.body

        Assert.assertEquals(expected.pages.size, actualBody.pages.size)
        expected.pages
                .sortedBy { it.order }
                .zip(actualBody.pages)
                .forEach { (e, a) ->
                    Assert.assertEquals(e.pageId, a.pageId)
                    Assert.assertEquals(e.notebookId, a.notebookId)
                    Assert.assertEquals(e.title, a.title)
                    Assert.assertEquals(e.order, a.order)
                    Assert.assertEquals(e.createdAt, a.createdAt)
                    Assert.assertEquals(e.updatedAt, a.updatedAt)

                }
    }
}