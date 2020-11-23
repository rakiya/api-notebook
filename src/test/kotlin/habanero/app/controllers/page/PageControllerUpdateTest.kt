package habanero.app.controllers.page

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.common.models.AccountEntity
import habanero.common.models.NotebookEntity
import habanero.common.models.NotebookEntity.Companion.createNext
import habanero.common.models.PageEntity
import habanero.common.models.PageEntity.Companion.createNext
import habanero.extensions.database.Tables
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.Page.PAGE
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
class PageControllerUpdateTest : ControllerTest() {

    data class Request(
            val notebookId: Long?,
            val title: String?,
            val order: Int?
    )

    data class Response(
            val pageId: Long,
            val notebookId: Long,
            val title: String,
            val order: Int,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
    )

    companion object {
        private val myAccount = AccountEntity("mine")
        private val othersAccount = AccountEntity("others")

        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
                    .createNext(myAccount.id)
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(1) // 自分のノート
                    .createNext(1) // 自分のノート
                    .createNext(1) // 自分のノート
                    .createNext(2) // 他人のノート
                    .createNext(3) // 自分のノート
                    .createNext(3) // 自分のノート
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(Notebook.NOTEBOOK).execute()
            db.batchInsert(notebooks.map { it.toRecord() }).execute()
        }
    }

    @BeforeEach
    fun beforeEach() {
        db.deleteFrom(Tables.PAGE).execute()
        db.batchInsert(pages.map { it.toRecord() }).execute()
    }

    @Test
    @DisplayName("タイトルを更新できること")
    fun `You can update a title`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(targetPage.notebookId, "変更されたタイトル", targetPage.order)

        put<Response, Request>("/page/${targetPage.pageId}", req, myAccount) {
            val expected = Response(
                    targetPage.pageId,
                    targetPage.notebookId,
                    req.title!!,
                    targetPage.order,
                    targetPage.createdAt.withNano(0),
                    it.body.updatedAt.withNano(0)
            )

            validate(expected, it)
        }
    }

    @Test
    @DisplayName("ページを他のノートに移動できること")
    fun `You can move a page to other notebooks`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val myNotebook = notebooks.first {
            it.id != targetPage.notebookId && it.accountId == myAccount.id
        }
        val req = Request(myNotebook.id, targetPage.title, targetPage.order)

        put<Response, Request>("/page/${targetPage.pageId}", req, myAccount) {
            val expected = Response(
                    targetPage.pageId,
                    myNotebook.id,
                    targetPage.title,
                    targetPage.order,
                    targetPage.createdAt.withNano(0),
                    it.body.updatedAt.withNano(0)
            )

            validate(expected, it)
        }
    }

    @Test
    @DisplayName("順序を変更できること")
    fun `You can update the order of a page`() {
        val targetPage = pages.last { it.notebookId == 1L }
        val req = Request(targetPage.notebookId, targetPage.title, targetPage.order - 1)

        put<Response, Request>("/page/${targetPage.pageId}", req, myAccount) { actual ->
            val expected = Response(
                    targetPage.pageId,
                    targetPage.notebookId,
                    targetPage.title,
                    req.order!!,
                    targetPage.createdAt.withNano(0),
                    actual.body.updatedAt.withNano(0)
            )

            validate(expected, actual)

            val actualOrders = db.selectFrom(PAGE)
                    .where(PAGE.NOTEBOOK_ID.eq(UInteger.valueOf(targetPage.notebookId)))
                    .orderBy(PAGE.ORDER.asc())
                    .fetch()
                    .into(PAGE)
                    .map { it.pageId.toLong() }

            val expectedOrders = pages
                    .filter { it.notebookId == targetPage.notebookId }
                    .sortedBy { it.order }
                    .map { it.pageId }
                    .toMutableList()
                    .also {
                        val tmp = it.last()
                        it[it.lastIndex] = it[it.lastIndex - 1]
                        it[it.lastIndex - 1] = tmp
                    }

            Assert.assertEquals(expectedOrders, actualOrders)
        }
    }

    @Test
    @DisplayName("リクエストにノートIDが含まれること")
    fun `A notebook ID should be in a request`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(notebookId = null, title = targetPage.title, order = targetPage.order)

        put<ErrorResponse, Request>("/page/${targetPage.pageId}", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("notebookId", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストにタイトルが含まれること")
    fun `A title should be in a request`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(notebookId = targetPage.notebookId, title = null, order = targetPage.order)

        put<ErrorResponse, Request>("/page/${targetPage.pageId}", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("title", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストに順序が含まれること")
    fun `A order should be in a request`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(notebookId = targetPage.notebookId, title = targetPage.title, order = null)

        put<ErrorResponse, Request>("/page/${targetPage.pageId}", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("order", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストのタイトルが256文字以下であること")
    fun `The title in request should be 256 chars or less`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val validReq = Request(targetPage.notebookId, "a".repeat(256), targetPage.order)

        put<Response, Request>("/page/${targetPage.pageId}", validReq, myAccount) {
            Assert.assertEquals(StatusCode.ACCEPTED_CODE, it.statusCode)
        }

        val invalidReq = Request(targetPage.notebookId, "b".repeat(257), targetPage.order)
        put<Response, Request>("/page/${targetPage.pageId}", invalidReq, myAccount) {
            Assert.assertEquals(StatusCode.BAD_REQUEST_CODE, it.statusCode)
        }
    }

    @Test
    @DisplayName("ログインが必要であること")
    fun `You cannot update without logging in`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(targetPage.notebookId, "変更", targetPage.order)

        put<Response, Request>("/page/${targetPage.pageId}", req) {
            Assert.assertEquals(StatusCode.UNAUTHORIZED_CODE, it.statusCode)
        }
    }

    @Test
    @DisplayName("ページを存在しないノートに移動できないこと")
    fun `You cannot move a page to a notebook not exited`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(9999, "変更", targetPage.order)

        put<ErrorResponse, Request>("/page/${targetPage.pageId}", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("notebookId", listOf("ノート(ID: 9999)は存在しません。"))

            validateFailure(expected, it)
        }
    }


    @Test
    @DisplayName("他人の所有するページを更新できないこと")
    fun `You cannot update others' pages`() {
        val othersNotebook = notebooks.first { it.accountId == othersAccount.id }
        val targetPage = pages.first { it.notebookId == othersNotebook.id }
        val req = Request(targetPage.notebookId, "変更", targetPage.order)

        put<Response, Request>("/page/${targetPage.pageId}", req, myAccount) {
            Assert.assertEquals(StatusCode.FORBIDDEN_CODE, it.statusCode)
        }
    }

    @Test
    @DisplayName("存在しないページを更新できないこと")
    fun `You cannot update a page not existed`() {
        val targetPage = pages.first { it.notebookId == 1L && it.order == 1 }
        val req = Request(targetPage.notebookId, "変更されたタイトル", targetPage.order)

        put<ErrorResponse, Request>("/page/9999", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                    .add("pageId", listOf("ページ(ID: 9999)は存在しません。"))

            validateFailure(expected, it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        // レスポンスを検証
        Assert.assertEquals(StatusCode.ACCEPTED_CODE, actual.statusCode)
        Assert.assertEquals(expected, actual.body)

        // データベースを検証
        val actualInDb = db.selectFrom(PAGE)
                .where(PAGE.PAGE_ID.eq(UInteger.valueOf(expected.pageId)))
                .fetchOne()
                ?.into(PAGE)

        Assert.assertNotNull(actualInDb)
        Assert.assertEquals(expected.notebookId, actualInDb!!.notebookId.toLong())
        Assert.assertEquals(expected.title, actualInDb.title)
        Assert.assertEquals(expected.order, actualInDb.order.toInt())
        Assert.assertEquals(expected.createdAt, actualInDb.createdAt)
        Assert.assertTrue(
                actualInDb.updatedAt.isAfter(actualInDb.createdAt) ||
                        actualInDb.updatedAt == actualInDb.createdAt
        )
    }
}