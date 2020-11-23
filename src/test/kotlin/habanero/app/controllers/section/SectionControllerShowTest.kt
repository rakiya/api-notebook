package habanero.app.controllers.section

import habanero.app.Application
import habanero.app.controllers.ControllerTest
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
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class SectionControllerShowTest : ControllerTest() {

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

        private val sections = runBlocking {
            mutableListOf<SectionEntity>()
                    .createNext(1)
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
    fun `You can get your section`() {
        val mySection = getSectionOwnedBy(myAccount)

        get<Response>("/section/${mySection.sectionId}", myAccount) {
            val expected = mySection.toRecord().into(Response::class.java)
            validate(expected, it)
        }
    }

    @Test
    @DisplayName("他人の所有するセクションを取得できないこと")
    fun `You cannot get others' section`() {
        val othersSection = getSectionOwnedBy(othersAccount)

        get<NoContentResponse>("/section/${othersSection.sectionId}", myAccount) {
            val expected = NoContentResponse(StatusCode.FORBIDDEN)
            validateNoContent(expected, it)
        }
    }

    @Test
    @DisplayName("存在しないセクションを取得できないこと")
    fun `You cannot get a section not existed`() {
        get<NoContentResponse>("/section/9999", myAccount) {
            val expected = NoContentResponse(StatusCode.NOT_FOUND)
            validateNoContent(expected, it)
        }
    }

    @Test
    @DisplayName("ログインせずに取得できないこと")
    fun `You should have logged in`() {
        val mySection = getSectionOwnedBy(myAccount)

        get<NoContentResponse>("/section/${mySection.sectionId}") {
            val expected = NoContentResponse(StatusCode.UNAUTHORIZED)
            validateNoContent(expected, it)
        }

    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)
        Assert.assertEquals(expected, actual.body)
    }

    /**
     * accountが所有するセクションを1つ取得する。
     *
     * @param account AccountEntity 所有者
     * @param nth Int 何番目を取得するか。デフォルトは一番最初に見つかったもの。
     * @return SectionEntity セクション
     */
    private fun getSectionOwnedBy(account: AccountEntity, nth: Int = 0): SectionEntity {
        val ownedNotebooks = notebooks
                .filter { it.accountId == account.id }
                .map { it.id }
        val ownedPages = pages
                .filter { ownedNotebooks.contains(it.notebookId) }
                .map { it.pageId }

        return sections.filter { ownedPages.contains(it.pageId) }[nth]
    }
}