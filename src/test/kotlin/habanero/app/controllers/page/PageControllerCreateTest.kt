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
import habanero.extensions.database.Tables.PAGE
import habanero.extensions.database.tables.Notebook.NOTEBOOK
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
class PageControllerCreateTest : ControllerTest() {

    data class Request(val title: String?, val order: Int?)

    data class Response(
            val pageId: Long,
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
    }

    @Test
    @DisplayName("自分が所有するノートにページを追加できること")
    fun `You can create a page`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val req = Request("abc", 1)

        post<Response, Request>("/notebook/${myNotebook.id}/page", req, myAccount) {
            val record = db.selectFrom(PAGE).fetchOne()
            val expected = Response(
                    record.pageId.toLong(),
                    req.title!!,
                    req.order!!,
                    record.createdAt,
                    record.updatedAt
            )

            validate(expected, it)
        }
    }

    @Test
    @DisplayName("他人が所有するノートにページを追加できないこと")
    fun `You cannot create a page in others' notebook`() {
        val othersNotebook = notebooks.first { it.accountId == othersAccount.id }
        val req = Request("abc", 1)

        val formerCount = db.selectCount().from(PAGE).fetchOne().component1()!!

        post<NoContentResponse, Request>("/notebook/${othersNotebook.id}/page", req, myAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)

            val count = db.selectCount().from(PAGE).fetchOne().component1()!!
            Assert.assertEquals(0, formerCount - count)
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val req = Request("abc", 1)

        val formerCount = db.selectCount().from(PAGE).fetchOne().component1()!!

        post<NoContentResponse, Request>("/notebook/${myNotebook.id}/page", req) {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)

            val count = db.selectCount().from(PAGE).fetchOne().component1()!!
            Assert.assertEquals(0, formerCount - count)
        }
    }

    @Test
    @DisplayName("タイトルは256文字以下でなければならない")
    fun `The Length of a title should be 256 or less`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }

        // 成功(256文字)
        val validReq = Request("あ".repeat(256), 1)
        post<Response, Request>("/notebook/${myNotebook.id}/page", validReq, myAccount) {
            val record = db.selectFrom(PAGE).fetchOne()
            val expected = Response(
                    record.pageId.toLong(),
                    validReq.title!!,
                    validReq.order!!,
                    record.createdAt,
                    record.updatedAt
            )

            validate(expected, it)
        }

        // 失敗(257文字)
        val invalidReq = Request("い".repeat(257), 1)
        val formerCount = db.selectCount().from(PAGE).fetchOne().component1()!!
        post<NoContentResponse, Request>("/notebook/${myNotebook.id}/page", invalidReq, myAccount) {
            validateNoContent(NoContentResponse(StatusCode.BAD_REQUEST), it)

            val presentCount = db.selectCount().from(PAGE).fetchOne().component1()!!
            Assert.assertEquals(0, formerCount - presentCount)

        }
    }

    @Test
    @DisplayName("リクエストにタイトルが必須であること")
    fun `A request should have a title`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val req = Request(null, 1)

        post<ErrorResponse, Request>("/notebook/${myNotebook.id}/page", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("title", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストに順序が必須であること")
    fun `A request should have a order`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val req = Request("タイトル1", null)

        post<ErrorResponse, Request>("/notebook/${myNotebook.id}/page", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("order", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("同じタイトルのページを作成できること")
    fun `You can add a page whose title is already registered`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val page = runBlocking {
            mutableListOf<PageEntity>().createNext(myNotebook.id).first().toRecord()
        }
        val req = Request(page.title, 1)

        // 下準備
        db.executeInsert(page)

        // 本処理
        post<Response, Request>("/notebook/${myNotebook.id}/page", req, myAccount) {
            val records = db.selectFrom(PAGE).fetch().into(PAGE)

            Assert.assertEquals(2, records.size)

            records.forEach {
                Assert.assertEquals(req.title!!, it.title)
            }
        }
    }

    @Test
    @DisplayName("ページ間に新しいページを追加できること")
    fun `You can add a page between pages`() {
        val myNotebook = notebooks.first { it.accountId == myAccount.id }
        val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(myNotebook.id) // order = 1
                    .createNext(myNotebook.id) // order = 2
                    .map { it.toRecord() }
        }
        val req = Request("abc", 2)

        // 下準備
        db.batchInsert(pages).execute()

        // 本処理
        post<Response, Request>("/notebook/${myNotebook.id}/page", req, myAccount) {
            val record = db.selectFrom(PAGE)
                    .where(PAGE.ORDER.eq(UInteger.valueOf(req.order!!)))
                    .limit(1)
                    .fetchOne()

            val expected = Response(
                    record.pageId.toLong(),
                    req.title!!,
                    req.order,
                    record.createdAt,
                    record.updatedAt
            )

            validate(expected, it)
        }
    }

    @Test
    @DisplayName("存在しないノートにページを追加できないこと")
    fun `You cannot add a page into a notebook not existed`() {
        val req = Request("abc", 1)

        post<ErrorResponse, Request>("/notebook/9999/page", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                    .add("notebookId", listOf("ノート(ID: 9999)は存在しません。"))

            validateFailure(expected, it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.CREATED_CODE, actual.statusCode)
        Assert.assertEquals(expected, actual.body)

        val actualInDb = db.selectFrom(PAGE)
                .where(PAGE.PAGE_ID.eq(UInteger.valueOf(expected.pageId)))
                .fetchOne()
                .into(PAGE)

        Assert.assertNotNull(actualInDb.pageId)
        Assert.assertNotNull(actualInDb.notebookId)
        Assert.assertEquals(expected.title, actualInDb.title)
        Assert.assertEquals(expected.order, actualInDb.order.toInt())
        Assert.assertEquals(actual.body.createdAt, actualInDb.createdAt)
        Assert.assertEquals(actual.body.updatedAt, actualInDb.updatedAt)
    }
}