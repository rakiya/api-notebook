package habanero.app.controllers.notebook

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
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
class NotebookControllerCreateTest : ControllerTest() {

    data class Request(val title: String?)

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
        private val anotherAccount = AccountEntity("user2")
        private val noisyData = listOf(
                NotebookRecord().apply {
                    notebookId = UInteger.valueOf(1)
                    accountId = myAccount.id
                    title = "重複するノート"
                    order = UInteger.valueOf(1)
                },
                NotebookRecord().apply {
                    notebookId = UInteger.valueOf(2)
                    accountId = myAccount.id
                    title = "重複しないノート"
                    order = UInteger.valueOf(2)
                },
                NotebookRecord().apply {
                    notebookId = UInteger.valueOf(3)
                    accountId = anotherAccount.id
                    title = "他人と重複するノート"
                    order = UInteger.valueOf(1)
                }
        )

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.delete(NOTEBOOK).execute()
            db.batchInsert(noisyData).execute()
        }
    }

    @Test
    @DisplayName("ノートを作成できること")
    fun `You can create a notebook`() = post<Response, Request>(
            "/notebook", Request("私のノート1"), myAccount
    ) {
        val expected = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.ACCOUNT_ID.eq(myAccount.id))
                .and(NOTEBOOK.TITLE.eq("私のノート1"))
                .fetchOne()
                .into(Response::class.java)

        validate(expected, it)
    }

    @Test
    @DisplayName("他人と同じタイトルのノートを作成できること")
    fun `You can create a notebook having a title duplicated with others one`() = post<Response, Request>(
            "/notebook", Request("他人と重複するノート"), myAccount
    ) {
        val expected = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.ACCOUNT_ID.eq(myAccount.id))
                .and(NOTEBOOK.TITLE.eq("他人と重複するノート"))
                .fetchOne()
                .into(Response::class.java)

        validate(expected, it)

    }

    @Test
    @DisplayName("ログインしなければノートを作成できないこと")
    fun `You should log in`() = post<Response, Request>(
            "/notebook", Request("私のノート2")
    ) {
        Assert.assertEquals(StatusCode.UNAUTHORIZED_CODE, it.statusCode)
        Assert.assertNull(it.body)
    }

    @Test
    @DisplayName("タイトルは256文字以下であること")
    fun `The length of titles should be 256 or less`() {
        post<Response, Request>("/notebook", Request("あ".repeat(256)), myAccount) {
            Assert.assertEquals(StatusCode.CREATED_CODE, it.statusCode)
        }

        post<ErrorResponse, Request>("/notebook", Request("い".repeat(257)), myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("title", listOf("256文字以下で入力してください。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("タイトルがNullでないこと")
    fun `Titles should not be null`() = post<ErrorResponse, Request>(
            "/notebook", Request(null), myAccount
    ) {
        val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                .add("title", listOf("必須項目です。"))
        validateFailure(expected, it)
    }

    @Test
    @DisplayName("タイトルがブランクでないこと")
    fun `Title should not be blank`() = post<ErrorResponse, Request>(
            "/notebook", Request(""), myAccount
    ) {
        val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                .add("title", listOf("必須項目です。"))
        validateFailure(expected, it)
    }

    @Test
    @DisplayName("重複するタイトルのノートが作成できないこと")
    fun `You cannot create notebooks having an existed title`() = post<ErrorResponse, Request>(
            "/notebook", Request("重複するノート"), myAccount
    ) {
        val expected = ErrorResponse(statusCode = StatusCode.CONFLICT)
                .add("title", listOf("既に存在します。"))

        validateFailure(expected, it)
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.CREATED_CODE, actual.statusCode)

        val actualBody = actual.body
        Assert.assertEquals(expected.notebookId, actualBody.notebookId)
        Assert.assertEquals(expected.title, actualBody.title)
        Assert.assertEquals(expected.order, actualBody.order)
        Assert.assertEquals(expected.createdAt, actualBody.createdAt)
        Assert.assertEquals(expected.updatedAt, actualBody.updatedAt)
    }
}