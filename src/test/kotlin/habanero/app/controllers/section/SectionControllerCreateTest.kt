package habanero.app.controllers.section

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
import habanero.extensions.database.tables.Section.SECTION
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
class SectionControllerCreateTest : ControllerTest() {

    data class Request(val content: String?)

    data class Response(
            val sectionId: Long,
            val pageId: Long,
            val content: String,
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
                    .toList()
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(1)
                    .createNext(1)
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
    }

    @Test
    @DisplayName("自分が所有するページ内にセクションを作成できること")
    fun `You can create new section on your page`() {
        val myPage = getPageOwnedBy(myAccount)
        val req = Request("初めてのセクションです。")

        validateRecordCount(1, SECTION) {
            post<Response, Request>("/page/${myPage.pageId}/section", req, myAccount) {
                validate(req, myPage.pageId, it)
            }
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val myPage = getPageOwnedBy(myAccount)
        val req = Request("初めてのセクションです。")

        validateRecordCount(0, SECTION) {
            post<NoContentResponse, Request>("/page/${myPage.pageId}/section", req) {
                validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
            }
        }
    }

    @Test
    @DisplayName("リクエストにコンテンツが含まれていること")
    fun `A content should be in a request`() {
        val myPage = getPageOwnedBy(myAccount)
        val req = Request(null)

        validateRecordCount(0, SECTION) {
            post<ErrorResponse, Request>("/page/${myPage.pageId}/section", req, myAccount) {
                val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                        .add("content", listOf("必須項目です。"))

                validateFailure(expected, it)
            }
        }
    }

    @Test
    @DisplayName("他人のページにセクションを作成できないこと")
    fun `You cannot create a section on an others' page`() {
        val othersPage = getPageOwnedBy(othersAccount)
        val req = Request("初めてのセクションです。")

        validateRecordCount(0, SECTION) {
            post<NoContentResponse, Request>("/page/${othersPage.pageId}/section", req, myAccount) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("存在しないページにセクションを作成できないこと")
    fun `You cannot create a section on a page not existed`() {
        val req = Request("初めてのセクションです。")

        validateRecordCount(0, SECTION) {
            post<ErrorResponse, Request>("/page/9999/section", req, myAccount) {
                val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                        .add("pageId", listOf("ページ(ID: 9999)が存在しません。"))

                validateFailure(expected, it)
            }
        }
    }

    fun validate(expected: Request, pageId: Long, actual: ResponseEntity<Response>) {
        // レスポンスコードを検証
        Assert.assertEquals(StatusCode.CREATED.value(), actual.statusCode)

        // DBのレコードを検証
        val actualInDb = db.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(UInteger.valueOf(actual.body.sectionId)))
                .fetchOneInto(SECTION)

        Assert.assertNotNull(actualInDb)
        Assert.assertEquals(pageId, actualInDb.pageId.toLong())
        Assert.assertEquals(expected.content, actualInDb.content)
        Assert.assertEquals(0, actualInDb.isTrashed.toInt())
        Assert.assertNotNull(actualInDb.createdAt)
        Assert.assertNotNull(actualInDb.updatedAt)

        // レスポンスボディを検証
        val expectedBody = Response(
                actualInDb.sectionId.toLong(),
                pageId, expected.content!!,
                actualInDb.createdAt,
                actualInDb.updatedAt
        )
        Assert.assertEquals(expectedBody, actual.body)
    }

    /**
     * IDがownerIdのアカウントが所有するページを取得する。
     *
     * @param ownerId AccountEntity 所有者のID
     * @param nth Int 何番目のページにするか
     * @return PageEntity ページ
     */
    private fun getPageOwnedBy(ownerId: AccountEntity, nth: Int = 0): PageEntity {
        val notebookIds = notebooks
                .filter { it.accountId == ownerId.id }
                .map { it.id }

        return pages.filter { notebookIds.contains(it.notebookId) }[nth]
    }
}