package habanero.app.controllers.trash

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
import habanero.common.models.SectionEntity.Companion.trashed
import habanero.extensions.database.Tables.*
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class TrashControllerIndexTest : ControllerTest() {

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
                    .createNext(2).trashed()
                    .createNext(3).trashed()
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
    @DisplayName("自分が所有する、ゴミ箱内のセクションを取得できること")
    fun `You can get all your sections in the trash`() {
        get<Response>("/trash", myAccount) { actual ->
            val expected = getTrashedSectionsOwnedBy(myAccount)
                    .map { it.toRecord().into(Response.ResponseInner::class.java) }
                    .let { Response(it) }

            validate(expected, actual)
        }
    }

    @Test
    @DisplayName("ログイン状態でないければならないこと")
    fun `You should have logged in`() {
        get<NoContentResponse>("/trash") {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)

        Assert.assertEquals(expected, actual.body)
    }

    private fun getTrashedSectionsOwnedBy(owner: AccountEntity): List<SectionEntity> {
        val notebookIds = notebooks
                .filter { it.accountId == owner.id }
                .map { it.id }

        val pageIds = pages.filter { notebookIds.contains(it.notebookId) }
                .map { it.pageId }

        return sections.filter { pageIds.contains(it.pageId) && it.isTrashed }
    }
}