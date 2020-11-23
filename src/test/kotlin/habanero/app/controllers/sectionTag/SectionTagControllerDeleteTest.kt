package habanero.app.controllers.sectionTag

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.NoContentResponse
import habanero.common.models.*
import habanero.common.models.NotebookEntity.Companion.createNext
import habanero.common.models.PageEntity.Companion.createNext
import habanero.common.models.SectionEntity.Companion.createNext
import habanero.common.models.SectionTagEntity.Companion.createNext
import habanero.common.models.TagEntity.Companion.createNext
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.Page.PAGE
import habanero.extensions.database.tables.Section.SECTION
import habanero.extensions.database.tables.SectionTag.SECTION_TAG
import habanero.extensions.database.tables.Tag.TAG
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@JoobyTest(Application::class)
class SectionTagControllerDeleteTest : ControllerTest() {

    companion object {
        private val myAccount = AccountEntity("mine")
        private val othersAccount = AccountEntity("others")

        // テストデータ
        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
                    .toList()
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(notebooks[0].id)
                    .createNext(notebooks[1].id)
                    .toList()
        }

        private val sections = runBlocking {
            mutableListOf<SectionEntity>()
                    .createNext(pages[0].pageId)
                    .createNext(pages[0].pageId)
                    .createNext(pages[1].pageId)
                    .toList()
        }

        private val tags = runBlocking {
            mutableListOf<TagEntity>()
                    .createNext(myAccount)
                    .createNext(myAccount)
                    .createNext(othersAccount)
        }

        private val sectionTags = kotlin.run {
            mutableListOf<SectionTagEntity>()
                    .createNext(sections[0], tags[0])
                    .createNext(sections[1], tags[1])
                    .createNext(sections[2], tags[2])
        }
    }

    @BeforeEach
    fun beforeEach() {
        listOf(NOTEBOOK, PAGE, SECTION, TAG, SECTION_TAG).forEach {
            db.deleteFrom(it).execute()
        }
        listOf(notebooks, pages, sections, tags, sectionTags).forEach { entities ->
            db.batchInsert(entities.map { it.toRecord() }).execute()
        }
    }

    @Test
    @DisplayName("自分のセクションのタグ付けを解除できること")
    fun `You can untag your section`() {
        val target = getSectionTagsOwnedBy(myAccount)[0]

        validateRecordCount(SECTION_TAG to -1) {
            delete<NoContentResponse>("/section/${target.sectionId}/tag/${target.tagId}", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.NO_CONTENT), it)
            }
        }
    }

    @Test
    @DisplayName("他人のセクションのタグ付を解除できないこと")
    fun `You cannot untag others' section`() {
        val target = getSectionTagsOwnedBy(othersAccount)[0]

        validateRecordCount(SECTION_TAG to 0) {
            delete<NoContentResponse>("/section/${target.sectionId}/tag/${target.tagId}", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("")

    private fun getSectionsOwnedBy(owner: AccountEntity): List<SectionEntity> {
        val notebookIds = notebooks
                .filter { it.accountId == owner.id }
                .map { it.id }

        val pageIds = pages
                .filter { notebookIds.contains(it.notebookId) }
                .map { it.pageId }

        return sections.filter { pageIds.contains(it.pageId) }
    }

    private fun getTagsOwnedBy(owner: AccountEntity): List<TagEntity> {
        return tags.filter { it.accountId == owner.id }
    }

    private fun getSectionTagsOwnedBy(owner: AccountEntity): List<SectionTagEntity> {
        val tagIds = tags
                .filter { it.accountId == owner.id }
                .map { it.tagId }

        return sectionTags.filter { tagIds.contains(it.tagId) }

    }
}