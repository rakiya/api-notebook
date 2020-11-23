package habanero.app.controllers.notebook

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.common.models.NotebookEntity
import habanero.common.models.NotebookEntity.Companion.createNext
import habanero.extensions.database.Tables.NOTEBOOK
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.records.NotebookRecord
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.jooq.types.UInteger
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class NotebookControllerUpdateTest : ControllerTest() {

    data class Request(val title: String?, val order: Int?)

    data class Response(
            val notebookId: Long,
            val title: String,
            val order: Int,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
    )

    companion object {
        /* テストデータ */
        private val myAccount = AccountEntity("user1")
        private val othersAccount = AccountEntity("user2")
        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
                    .createNext(myAccount.id)
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(Notebook.NOTEBOOK).execute()
        }
    }

    @BeforeEach
    fun beforeEach() {
        db.deleteFrom(NOTEBOOK).execute()
        db.batchInsert(notebooks.map { it.toRecord() }).execute()
    }

    @Test
    @DisplayName("ノートのタイトルを変更できること")
    fun `You can update the title of a notebook`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val updatedNotebook = myNotebook.copy(title = "変更されたノート")

        put<Response, Request>(
                "/notebook/${myNotebook.id}",
                Request(updatedNotebook.title, myNotebook.order.toInt()),
                myAccount
        ) {
            val actualRecord = db.selectFrom(NOTEBOOK)
                    .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(myNotebook.id)))
                    .fetchOne()
                    .into(NOTEBOOK)!!

            validateResponse(updatedNotebook.toRecord().into(Response::class.java), it)
            validateRecord(updatedNotebook.toRecord(), actualRecord)
        }
    }

    @Test
    @DisplayName("ノートの順序を変更できること")
    fun `You can update the order of a notebook`() {
        val myNotebook = notebooks.last { it.accountId == myAccount.id }
        val updatedNotebook = myNotebook.copy(order = 1)

        put<Response, Request>(
                "/notebook/${myNotebook.id}",
                Request(myNotebook.title, updatedNotebook.order),
                myAccount
        ) {
            val actualRecord = db.selectFrom(NOTEBOOK)
                    .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(myNotebook.id)))
                    .fetchOne()
                    .into(NOTEBOOK)!!

            validateResponse(updatedNotebook.toRecord().into(Response::class.java), it)
            validateRecord(updatedNotebook.toRecord(), actualRecord)
        }
    }

    @Test
    @DisplayName("既に存在するノート名に変更できないこと")
    fun `You cannot update a notebook to an existed title`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val myNotebook2 = notebooks.last { it.accountId == myAccount.id }

        put<ErrorResponse, Request>(
                "/notebook/${myNotebook.id}",
                Request(myNotebook2.title, myNotebook.order),
                myAccount
        ) {
            val expected = ErrorResponse(statusCode = StatusCode.CONFLICT)
                    .add("title", listOf("既に存在します。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストにタイトルを指定すること")
    fun `A title should be in a request`() {
        val myNotebook = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(1)))
                .fetchOne()!!

        put<ErrorResponse, Request>(
                "/notebook/${myNotebook.notebookId}",
                Request(null, myNotebook.order.toInt()),
                myAccount
        ) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("title", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストに順序が指定されていること")
    fun `An order should be in a request`() {
        val myNotebook = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(1)))
                .fetchOne()!!

        put<ErrorResponse, Request>(
                "/notebook/${myNotebook.notebookId}",
                Request("変更されたノート", null),
                myAccount
        ) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("order", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }


    @Test
    @DisplayName("リクエストのタイトルの文字数が256文字以上にならないこと")
    fun `The length of a title should be 256 or less`() {
        val myNotebook = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(1)))
                .fetchOne()!!

        put<Response, Request>(
                "/notebook/${myNotebook.notebookId}",
                Request("あ".repeat(256), myNotebook.order.toInt()),
                myAccount
        ) {
            Assert.assertEquals(StatusCode.ACCEPTED_CODE, it.statusCode)
        }

        put<ErrorResponse, Request>(
                "/notebook/${myNotebook.notebookId}",
                Request("い".repeat(257), myNotebook.order.toInt()),
                myAccount
        ) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("title", listOf("256文字以下で入力してください。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("他人のノートを変更できないこと")
    fun `You cannot update others' notebook`() {
        val othersNotebook = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.ACCOUNT_ID.eq(othersAccount.id))
                .limit(1)
                .fetchOne()


        put<NoContentResponse, Request>(
                "/notebook/${othersNotebook.notebookId}",
                Request("私のノート2", othersNotebook.order.toInt()),
                myAccount
        ) {
            val expected = NoContentResponse(statusCode = StatusCode.FORBIDDEN)

            validateNoContent(expected, it)
        }
    }

    @Test
    @DisplayName("存在しないノートを更新できないこと")
    fun `You cannot update a notebook not existed`() = put<ErrorResponse, Request>(
            "/notebook/99999", Request("変更", 1), myAccount
    ) {
        val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                .add("notebookId", listOf("ノート(ID: 9999)が存在しません。"))
        validateFailure(expected, it)
    }

    private fun validateResponse(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.ACCEPTED_CODE, actual.statusCode)

        val actualBody = actual.body
        Assert.assertEquals(expected.notebookId, actualBody.notebookId)
        Assert.assertEquals(expected.title, actualBody.title)
        Assert.assertEquals(expected.order, actualBody.order)
        Assert.assertEquals(expected.createdAt, actualBody.createdAt)
        Assert.assertTrue(actualBody.updatedAt == expected.createdAt || actualBody.updatedAt.isAfter(expected.createdAt))
    }

    private fun validateRecord(expected: NotebookRecord, actual: NotebookRecord) {
        Assert.assertEquals(expected.notebookId, actual.notebookId)
        Assert.assertEquals(expected.accountId, actual.accountId)
        Assert.assertEquals(expected.order, actual.order)
        Assert.assertEquals(expected.createdAt, actual.createdAt)
        Assert.assertTrue(actual.updatedAt.isAfter(actual.createdAt) || actual.updatedAt == actual.createdAt)
    }
}
