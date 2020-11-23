package habanero.app.controllers.sectionTag

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
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
import org.jooq.types.UInteger
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@JoobyTest(Application::class)
class SectionTagControllerCreateTest : ControllerTest() {

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
    @DisplayName("自分のセクションにタグ付けできること")
    fun `You can tag your section`() {
        val mySection = getSectionsOwnedBy(myAccount)[0]
        val unusedTagIds = sectionTags
                .filter { it.sectionId != mySection.sectionId }
                .map { it.tagId }
        val myTag = getTagsOwnedBy(myAccount).filter { unusedTagIds.contains(it.tagId) }[0]

        validateRecordCount(SECTION_TAG to 1) {
            post<NoContentResponse, Any>(
                    "/section/${mySection.sectionId}/tag/${myTag.tagId}",
                    null,
                    myAccount
            ) { validate(mySection, myTag, it) }
        }
    }

    @Test
    @DisplayName("他のセクションに付いているタグを付けられること")
    fun `You can tag your section with tag of other sections`() {
        val mySection = getSectionsOwnedBy(myAccount)[0]
        val usedTagIds = sectionTags
                .filter { it.sectionId != mySection.sectionId }
                .map { it.tagId }
        val myTag = getTagsOwnedBy(myAccount).filter { usedTagIds.contains(it.tagId) }[0]

        validateRecordCount(SECTION_TAG to 1) {
            post<NoContentResponse, Any>(
                    "/section/${mySection.sectionId}/tag/${myTag.tagId}",
                    null,
                    myAccount
            ) { validate(mySection, myTag, it) }
        }
    }

    @Test
    @DisplayName("同じタグを同じセクションに2回以上付けられないこと")
    fun `You cannot tag a same section with the same tag`() {
        val myTagIds = getTagsOwnedBy(myAccount).map { it.tagId }
        val mySectionTag = sectionTags.filter { myTagIds.contains(it.tagId) }[0]
        val mySection = sections.first { it.sectionId == mySectionTag.sectionId }
        val myTag = tags.first { it.tagId == mySectionTag.tagId }

        validateRecordCount(SECTION_TAG to 0) {
            post<ErrorResponse, Any>(
                    "/section/${mySection.sectionId}/tag/${myTag.tagId}",
                    null,
                    myAccount
            ) {
                val expected = ErrorResponse(statusCode = StatusCode.CONFLICT)
                        .add("tag", listOf("タグ(ID: ${myTag.tagId}は既にセクション(ID: ${mySection.sectionId}にタグ付けされています。"))
                validateFailure(expected, it)
            }
        }
    }

    @Test
    @DisplayName("他人のタグを付けられないこと")
    fun `You cannot tag your section with others' tag`() {
        val mySection = getSectionsOwnedBy(myAccount)[0]
        val othersTag = getTagsOwnedBy(othersAccount)[0]

        validateRecordCount(SECTION_TAG to 0) {
            post<NoContentResponse, Any>(
                    "/section/${mySection.sectionId}/tag/${othersTag.tagId}",
                    null,
                    myAccount
            ) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("他人のセクションに自分のタグを付けられないこと")
    fun `You cannot tag others section with your tag`() {
        val othersSection = getSectionsOwnedBy(othersAccount)[0]
        val myTag = getTagsOwnedBy(myAccount)[0]

        validateRecordCount(SECTION_TAG to 0) {
            post<NoContentResponse, Any>(
                    "/section/${othersSection.sectionId}/tag/${myTag.tagId}",
                    null,
                    myAccount
            ) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("他人のセクションに他人のタグを付けられないこと")
    fun `You cannot tag others section with others' tag`() {
        val othersSection = getSectionsOwnedBy(othersAccount)[0]
        val othersTag = getTagsOwnedBy(othersAccount)[0]

        validateRecordCount(SECTION_TAG to 0) {
            post<NoContentResponse, Any>(
                    "/section/${othersSection.sectionId}/tag/${othersTag.tagId}",
                    null,
                    myAccount
            ) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("存在しないタグを付けられないこと")
    fun `You cannot tag your section with tag not existed`() {
        val mySection = getSectionsOwnedBy(myAccount)[0]

        validateRecordCount(SECTION_TAG to 0) {
            post<ErrorResponse, Any>(
                    "/section/${mySection.sectionId}/tag/9999",
                    null,
                    myAccount
            ) {
                val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                        .add("tagId", listOf("タグ(ID: 9999)は存在しません。"))

                validateFailure(expected, it)
            }
        }
    }

    @Test
    @DisplayName("他人のセクションに他人のタグを付けられないこと")
    fun `You cannot tag section not existed`() {
        val myTag = getTagsOwnedBy(myAccount)[0]

        validateRecordCount(SECTION_TAG to 0) {
            post<ErrorResponse, Any>(
                    "/section/9999/tag/${myTag.tagId}",
                    null,
                    myAccount
            ) {
                val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                        .add("sectionId", listOf("セクション(ID: 9999)は存在しません。"))

                validateFailure(expected, it)
            }
        }
    }

    private fun validate(section: SectionEntity, tag: TagEntity, actual: ResponseEntity<NoContentResponse>) {
        Assert.assertEquals(StatusCode.CREATED.value(), actual.statusCode)

        val actualInDb = db.selectFrom(SECTION_TAG)
                .where(SECTION_TAG.SECTION_ID.eq(UInteger.valueOf(section.sectionId)))
                .and(SECTION_TAG.TAG_ID.eq(UInteger.valueOf(tag.tagId)))
                .fetchOne()

        Assert.assertNotNull(actualInDb)
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

    private fun getTagsOwnedBy(owner: AccountEntity): List<TagEntity> {
        return tags.filter { it.accountId == owner.id }
    }
}