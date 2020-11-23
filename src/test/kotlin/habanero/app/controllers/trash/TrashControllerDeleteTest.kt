package habanero.app.controllers.trash

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
import habanero.extensions.database.tables.records.SectionRecord
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.jooq.types.UInteger
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@JoobyTest(Application::class)
class TrashControllerDeleteTest : ControllerTest() {

    companion object {
        val myAccount = AccountEntity("mine")
        val othersAccount = AccountEntity("others")

        val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
        }

        val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(1)
                    .createNext(2)
        }

        val sections = runBlocking {
            mutableListOf<SectionEntity>()
                    .createNext(1)
                    .createNext(2)
        }
    }

    @BeforeEach
    fun beforeEach() {
        listOf(NOTEBOOK, PAGE, SECTION).forEach { db.deleteFrom(it).execute() }

        listOf(notebooks, pages, sections).forEach { entities -> db.batchInsert(entities.map { it.toRecord() }).execute() }
    }

    @Test
    @DisplayName("セクションをゴミ箱から取り出せること")
    fun `You can pick up your section from the trash`() {
        val target = getSectionOwnedBy(myAccount)[0]

        delete<NoContentResponse>("/trash/${target.sectionId}", myAccount) {
            validateWhenSuccess(target.toRecord(), it.statusCode)
        }
    }

    @Test
    @DisplayName("ゴミ箱にないセクションに対して操作できること")
    fun `You can pick up your section at out of the trash`() {
        val target = getSectionOwnedBy(myAccount)[0]

        db.update(SECTION)
                .set(SECTION.IS_TRASHED, 1)
                .where(SECTION.SECTION_ID.eq(UInteger.valueOf(target.sectionId)))
                .execute()


        delete<NoContentResponse>("/trash/${target.sectionId}", myAccount) {
            validateWhenSuccess(target.toRecord(), it.statusCode)
        }
    }

    @Test
    @DisplayName("ログインしなければんならないこと")
    fun `You should have logged in`() {
        val target = getSectionOwnedBy(myAccount)[0]

        delete<NoContentResponse>("/trash/${target.sectionId}") {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
            validateDbWhenFailure(target.toRecord())
        }
    }

    @Test
    @DisplayName("他人のセクションをゴミ箱に移動できないこと")
    fun `You cannot pick up others' section from the trash`() {
        val target = getSectionOwnedBy(othersAccount)[0]

        delete<NoContentResponse>("/trash/${target.sectionId}", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            validateDbWhenFailure(target.toRecord())
        }
    }

    @Test
    @DisplayName("存在しないセクションをゴミ箱に移動できないこと")
    fun `You cannot pick up a section not existed`() {
        delete<ErrorResponse>("/trash/9999", myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                    .add("sectionId", listOf("セクション(ID: 9999)は存在しません。"))

            validateFailure(expected, it)
        }
    }


    /**
     * リクエストが成功したとき、レスポンスコードとデータベースの状態を検証する。
     *
     * @param expected SectionRecord 期待値
     * @param actualStatusCode Int 実際のステータスコード
     */
    private fun validateWhenSuccess(expected: SectionRecord, actualStatusCode: Int) {
        Assert.assertEquals(StatusCode.NO_CONTENT.value(), actualStatusCode)

        val actual = db.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(expected.sectionId))
                .fetchOneInto(SECTION)

        Assert.assertNotNull(actual)
        Assert.assertEquals(expected.pageId, actual.pageId)
        Assert.assertEquals(expected.content, actual.content)
        Assert.assertEquals(0, actual.isTrashed.toInt())
        Assert.assertEquals(expected.createdAt, actual.createdAt)
    }

    private fun validateDbWhenFailure(expected: SectionRecord) {
        val actual = db.selectFrom(SECTION)
                .where(SECTION.SECTION_ID.eq(expected.sectionId))
                .fetchOneInto(SECTION)

        Assert.assertNotNull(actual)
        Assert.assertEquals(expected, actual)
    }


    private fun getSectionOwnedBy(owner: AccountEntity): List<SectionEntity> {
        val notebookIds = notebooks
                .filter { it.accountId == owner.id }
                .map { it.id }

        val pageIds = pages
                .filter { notebookIds.contains(it.notebookId) }
                .map { it.pageId }

        return sections.filter { pageIds.contains(it.pageId) }
    }
}