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
import habanero.common.models.SectionEntity.Companion.trashed
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.Page.PAGE
import habanero.extensions.database.tables.Section.SECTION
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class SectionControllerIndexTest : ControllerTest() {

    data class Response(val sections: List<ResponseInner>) {
        data class ResponseInner(
                val sectionId: Long,
                val pageId: Long,
                val content: String,
                val createdAt: LocalDateTime,
                val updatedAt: LocalDateTime
        )
    }

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

        private val sections = runBlocking {
            mutableListOf<SectionEntity>()
                    .createNext(1).trashed()
                    .createNext(1)
                    .createNext(2)
                    .createNext(3)
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(NOTEBOOK).execute()
            db.deleteFrom(PAGE).execute()
            db.deleteFrom(SECTION).execute()
            db.batchInsert(notebooks.map { it.toRecord() }).execute()
            db.batchInsert(pages.map { it.toRecord() }).execute()
            db.batchInsert(sections.map { it.toRecord() }).execute()
        }
    }

    @Test
    @DisplayName("自分の所有するセクションを取得できること")
    fun `You can get all your sections`() {
        val myPage = getPageOwnedBy(myAccount)

        get<Response>("/page/${myPage.pageId}/sections", myAccount) { actual ->
            val expected = sections
                    .filter { it.pageId == myPage.pageId && !it.isTrashed }
                    .map { it.toRecord().into(Response.ResponseInner::class.java)!! }
                    .let { Response(it) }

            validate(expected, actual)
        }
    }

    @Test
    @DisplayName("他人の所有するセクションを取得できないこと")
    fun `You cannot get others' sections`() {
        val myPage = getPageOwnedBy(othersAccount)

        get<NoContentResponse>("/page/${myPage.pageId}/sections", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
        }
    }

    @Test
    @DisplayName("存在しないページのセクションを取得できないこと")
    fun `You cannot get sections in pages not existed`() {
        get<ErrorResponse>("/page/9999/sections", myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                    .add("pageId", listOf("ページ(ID: 9999)が存在しません。"))
            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("ログインせずに取得できないこと")
    fun `You should have logged in`() {
        val myPage = getPageOwnedBy(myAccount)

        get<NoContentResponse>("/page/${myPage.pageId}/sections") {
            val expected = NoContentResponse(StatusCode.UNAUTHORIZED)

            validateNoContent(expected, it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)
        Assert.assertEquals(expected, actual.body)
    }

    private fun getPageOwnedBy(owner: AccountEntity, offset: Int = 0): PageEntity {
        val notebookIds = notebooks
                .filter { it.accountId == owner.id }
                .map { it.id }

        return pages.filter { notebookIds.contains(it.notebookId) }[offset]

    }
}