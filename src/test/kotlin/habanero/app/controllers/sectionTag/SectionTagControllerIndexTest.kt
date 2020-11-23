package habanero.app.controllers.sectionTag

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.NoContentResponse
import habanero.common.models.*
import habanero.common.models.NotebookEntity.Companion.createNext
import habanero.common.models.PageEntity.Companion.createNext
import habanero.common.models.SectionEntity.Companion.createNext
import habanero.common.models.SectionTagEntity.Companion.createNext
import habanero.common.models.SectionTagEntity.Companion.of
import habanero.common.models.TagEntity.Companion.createNext
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.Page.PAGE
import habanero.extensions.database.tables.Section.SECTION
import habanero.extensions.database.tables.SectionTag.SECTION_TAG
import habanero.extensions.database.tables.Tag.TAG
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class SectionTagControllerIndexTest : ControllerTest() {

    data class Response(
            val sectionTags: List<ResponseInner>
    ) {
        data class ResponseInner(
                val sectionId: Long,
                val tags: List<ResponseInner2>
        ) {
            data class ResponseInner2(
                    val tagId: Long,
                    val name: String,
                    val createdAt: LocalDateTime,
                    val updatedAt: LocalDateTime
            )
        }
    }

    companion object {
        private val myAccount = AccountEntity("mine")
        private val othersAccount = AccountEntity("others")

        // テストデータ
        private val notebooks = runBlocking {
            mutableListOf<NotebookEntity>()
                    .createNext(myAccount.id)
                    .createNext(myAccount.id)
                    .createNext(othersAccount.id)
                    .toList()
        }

        private val pages = runBlocking {
            mutableListOf<PageEntity>()
                    .createNext(notebooks[0].id)
                    .createNext(notebooks[1].id)
                    .createNext(notebooks[2].id)
                    .toList()
        }

        private val sections = runBlocking {
            mutableListOf<SectionEntity>()
                    .createNext(pages[0].pageId)
                    .createNext(pages[0].pageId)
                    .createNext(pages[1].pageId)
                    .createNext(pages[2].pageId)
                    .toList()
        }

        private val tags = runBlocking {
            mutableListOf<TagEntity>()
                    .createNext(myAccount)
                    .createNext(myAccount)
                    .createNext(myAccount)
                    .createNext(othersAccount)
        }

        private val sectionTags = kotlin.run {
            mutableListOf<SectionTagEntity>()
                    .createNext(sections[0], tags[0])
                    .createNext(sections[0], tags[1])
                    .createNext(sections[1], tags[1])
                    .createNext(sections[1], tags[2])
                    .createNext(sections[3], tags[3])
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            listOf(NOTEBOOK, PAGE, SECTION, TAG, SECTION_TAG).forEach {
                db.deleteFrom(it).execute()
            }
            listOf(notebooks, pages, sections, tags, sectionTags).forEach { entities ->
                db.batchInsert(entities.map { it.toRecord() }).execute()
            }
        }
    }

    @Test
    @DisplayName("セクションに付けられたタグを全て取得できること")
    fun `You can get all tags of your section`() {
        val target = getSectionsOwnedBy(myAccount)[0]

        get<Response>("/section/${target.sectionId}/tags", myAccount) { actual ->
            val expected = getTagsOfSectionAndOwnedBy(target, myAccount)
                    .map { it.toRecord().into(Response.ResponseInner.ResponseInner2::class.java) }
                    .let { Response.ResponseInner(target.sectionId, it) }
                    .let { Response(listOf(it)) }

            validate(expected, actual)
        }
    }

    @Test
    @DisplayName("タグ付けされていないセクションのタグを取得できること")
    fun `You can get tags of your no-tagged section`() {
        val sectionIdsWithTags = sectionTags.map { it.sectionId }.distinct()
        val target = getSectionsOwnedBy(myAccount)
                .filter { !sectionIdsWithTags.contains(it.sectionId) }[0]

        get<Response>("/section/${target.sectionId}/tags", myAccount) { actual ->
            val expected = Response(listOf())

            validate(expected, actual)
        }
    }

    @Test
    @DisplayName("他人のセクションのタグを取得できないこと")
    fun `You cannot get tags of others section`() {
        val target = getSectionsOwnedBy(othersAccount)[0]

        get<NoContentResponse>("/section/${target.sectionId}/tags", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
        }
    }

    @Test
    @DisplayName("存在しないセクションのタグを取得できないこと")
    fun `You cannot get tags of a section not existed`() {
        get<NoContentResponse>("/section/9999/tags", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.NOT_FOUND), it)
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val target = getSectionsOwnedBy(myAccount)[0]

        get<NoContentResponse>("/section/${target.sectionId}/tags") {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)
        Assert.assertEquals(expected, actual.body)
    }

    private fun getSectionsOwnedBy(owner: AccountEntity): List<SectionEntity> {
        val notebookIds = notebooks
                .filter { it.accountId == owner.id }
                .map { it.id }

        val pageIds = pages
                .filter { notebookIds.contains(it.notebookId) }
                .map { it.pageId }

        return sections.filter { pageIds.contains(it.pageId) }
    }

    private fun getTagsOfSectionAndOwnedBy(section: SectionEntity, owner: AccountEntity): List<TagEntity> {
        val ownedTags = tags.filter { it.accountId == owner.id }
        val tagIdsOfSection = sectionTags.of(section.sectionId).map { it.tagId }

        return ownedTags.filter { tagIdsOfSection.contains(it.tagId) }
    }

}