package habanero.app.controllers.notebook

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.records.NotebookRecord
import io.jooby.JoobyTest
import io.jooby.StatusCode
import org.jooq.types.UInteger
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class NotebookControllerShowTest : ControllerTest() {

    data class Response(
            val notebookId: Long,
            val title: String,
            val order: Int,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
    )

    companion object {
        private val myAccount = AccountEntity("user1")
        private val anotherAccount = AccountEntity("user2")

        private val data = listOf(
                NotebookRecord().apply {
                    notebookId = UInteger.valueOf(1)
                    accountId = myAccount.id
                    title = "プライベート"
                    order = UInteger.valueOf(1)
                },
                NotebookRecord().apply {
                    notebookId = UInteger.valueOf(2)
                    accountId = anotherAccount.id
                    title = "他人のノート"
                    order = UInteger.valueOf(1)
                },
                NotebookRecord().apply {
                    notebookId = UInteger.valueOf(3)
                    accountId = myAccount.id
                    title = "パブリック"
                    order = UInteger.valueOf(2)
                }
        )

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            db.delete(NOTEBOOK).execute()
            db.batchInsert(data).execute()
        }
    }

    @Test
    @DisplayName("ノートが取得できること")
    fun `You can get a notebook`() = get<Response>(
            "/notebook/${data[0].notebookId}", myAccount
    ) {
        val expected = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.TITLE.eq(data[0].title))
                .and(NOTEBOOK.ORDER.eq(data[0].order))
                .fetchOne()
                .into(Response::class.java)

        validate(expected, it)
    }

    @Test
    @DisplayName("他人のノートを取得できないこと")
    fun `You cannot get the others notebooks`() = get<NoContentResponse>(
            "/notebook/${data[1].notebookId}", myAccount
    ) {
        validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
    }

    @Test
    @DisplayName("存在しないノートを取得できないこと")
    fun `You cannot notebooks not existed`() = get<ErrorResponse>("/notebook/99999", myAccount) {
        val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                .add("notebookId", listOf("ノート(ID: 9999)が存在しません。"))

        validateFailure(expected, it)
    }

    @Test
    @DisplayName("パスパラメータの型が間違えているとき、Bad Requestになること")
    fun `Bad Request should be returned when a invalid path param`() =
            get<Response?>("/notebook/abc", myAccount) {
                Assert.assertEquals(StatusCode.BAD_REQUEST_CODE, it.statusCode)
                Assert.assertNull(it.body)
            }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK_CODE, actual.statusCode)

        val actualBody = actual.body
        Assert.assertEquals(expected.notebookId, actualBody.notebookId)
        Assert.assertEquals(expected.title, actualBody.title)
        Assert.assertEquals(expected.order, actualBody.order)
        Assert.assertEquals(expected.createdAt, actualBody.createdAt)
        Assert.assertEquals(expected.updatedAt, actualBody.updatedAt)
    }
}