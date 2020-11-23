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
import habanero.common.models.SectionEntity
import habanero.common.models.SectionEntity.Companion.createNext
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
class SectionControllerUpdateTest : ControllerTest() {

    data class Request(val pageId: Long?, val content: String?)

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
    @DisplayName("自分が所有するセクションを更新できること")
    fun `You can update your section`() {
        val (pageId, sectionId) = insertSection(myAccount)
        val req = Request(pageId, "変更されたコンテンツです。")

        put<Response, Request>("/section/$sectionId", req, myAccount) {
            val expected = Response(
                    sectionId,
                    pageId,
                    req.content!!,
                    it.body.createdAt,
                    it.body.updatedAt
            )

            validate(expected, it)
        }
    }

    @Test
    @DisplayName("他人の所有するセクションを更新できないこと")
    fun `You cannot update others' section`() {
        val (pageId, sectionId) = insertSection(othersAccount)
        val req = Request(pageId, "他人のコンテンツなので変更できません。")

        put<NoContentResponse, Request>("/section/$sectionId", req, myAccount) {
            val expected = NoContentResponse(StatusCode.FORBIDDEN)
            validateNoContent(expected, it)
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val (pageId, sectionId) = insertSection(myAccount)
        val req = Request(pageId, "変更できません。")

        put<NoContentResponse, Request>("/section/$sectionId", req) {
            val expected = NoContentResponse(StatusCode.UNAUTHORIZED)
            validateNoContent(expected, it)
        }
    }

    @Test
    @DisplayName("存在しないセクションを変更できないこと")
    fun `You cannot update a section not existed`() {
        val (pageId, _) = insertSection(myAccount)
        val req = Request(pageId, "変更できません。")

        put<NoContentResponse, Request>("/section/9999", req, myAccount) {
            val expected = NoContentResponse(StatusCode.NOT_FOUND)
            validateNoContent(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストにページIDが含まれていること")
    fun `A page ID should be in a request`() {
        val (_, sectionId) = insertSection(myAccount)
        val req = Request(null, "変更できません。")

        put<ErrorResponse, Request>("/section/$sectionId", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("pageId", listOf("必須項目です。"))
            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストにコンテンツが含まれていること")
    fun `A content should be in a request`() {
        val (pageId, sectionId) = insertSection(myAccount)
        val req = Request(pageId, null)

        put<ErrorResponse, Request>("/section/$sectionId", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("content", listOf("必須項目です。"))
            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("セクションを存在しないページに移動できないこと")
    fun `You cannot move a section to a page not existed`() {
        val (_, sectionId) = insertSection(myAccount)
        val req = Request(9999, "エラーになります。")

        put<ErrorResponse, Request>("/section/$sectionId", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("pageId", listOf("ページ(ID: 9999)は存在しません。"))
            validateFailure(expected, it)
        }
    }

    /**
     * IDがownerIdのアカウントが所有するnthPage番目のノートにセクションを追加する。
     *
     * @param owner AccountEntity 所有者のID
     * @param nthPage Int 何番目のページに追加するか
     * @return Pair<Long, Long> セクションを追加したページのID、追加したセクションのID
     */
    private fun insertSection(owner: AccountEntity, nthPage: Int = 0): Pair<Long, Long> {
        val notebookIds = notebooks
                .filter { it.accountId == owner.id }
                .map { it.id }

        val page = pages.filter { notebookIds.contains(it.notebookId) }[nthPage]

        val section = runBlocking {
            mutableListOf<SectionEntity>().createNext(page.pageId)[0].toRecord()
        }

        val sectionId = db.insertInto(SECTION).set(section)
                .returningResult(SECTION.SECTION_ID)
                .fetchOne()
                .component1()
                .toLong()

        return page.pageId to sectionId
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        // ステータスコードを検証
        Assert.assertEquals(StatusCode.ACCEPTED_CODE, actual.statusCode)

        // DBのデータを検証
        val actualInDb = db.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(UInteger.valueOf(expected.sectionId)))
                .fetchOneInto(SECTION)

        Assert.assertNotNull(actualInDb)
        Assert.assertEquals(expected.pageId, actualInDb.pageId.toLong())
        Assert.assertEquals(expected.content, actualInDb.content)
        Assert.assertTrue(
                actualInDb.updatedAt.isAfter(actualInDb.createdAt)
                        || actualInDb.updatedAt == actualInDb.createdAt
        )

        // HTTPボディを検証
        Assert.assertEquals(expected, actual.body)
    }
}