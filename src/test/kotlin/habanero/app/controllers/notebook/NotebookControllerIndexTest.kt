package habanero.app.controllers.notebook

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.common.models.AccountEntity
import habanero.extensions.database.tables.Notebook.NOTEBOOK
import habanero.extensions.database.tables.records.NotebookRecord
import io.jooby.JoobyTest
import io.jooby.StatusCode
import org.jooq.types.UInteger
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class NotebookControllerIndexTest : ControllerTest() {

    data class Response(val notebooks: List<ResponseInner>) {
        data class ResponseInner(
                val notebookId: Long,
                val title: String,
                val order: Int,
                val createdAt: LocalDateTime,
                val updatedAt: LocalDateTime
        )
    }

    companion object {
        private val myAccount = AccountEntity("user1")
        private val anotherAccount = AccountEntity("user2")

        private val data = listOf(
                NotebookRecord().apply {
                    accountId = myAccount.id
                    title = "プライベート"
                    order = UInteger.valueOf(1)
                },
                NotebookRecord().apply {
                    accountId = anotherAccount.id
                    title = "他人のノート"
                    order = UInteger.valueOf(1)
                },
                NotebookRecord().apply {
                    accountId = myAccount.id
                    title = "パブリック"
                    order = UInteger.valueOf(2)
                }
        )

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            db.delete(NOTEBOOK).execute()
            db.batchInsert(data).execute()
        }
    }

    @Test
    @DisplayName("ノートを取得できること")
    fun `You can get your notebooks`() = get<Response>("/notebooks", myAccount) {
        val expected =
                db.selectFrom(NOTEBOOK)
                        .where(NOTEBOOK.ACCOUNT_ID.eq(myAccount.id))
                        .orderBy(NOTEBOOK.ORDER)
                        .fetch()
                        .into(Response.ResponseInner::class.java)
                        .let { Response(it) }

        validate(it, expected)
    }

    @Test
    @DisplayName("他人のノートを取得できないこと")
    fun `You cannot get the other's notebook`() = get<Response>("/notebooks", myAccount) { response ->
        val notebookIds = response.body.notebooks.map { UInteger.valueOf(it.notebookId) }
        val accountIds = db.select(NOTEBOOK.ACCOUNT_ID)
                .from(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.`in`(notebookIds))
                .fetch()
                .into(String::class.java)

        accountIds.forEach { actual ->
            if (actual != myAccount.id) Assert.fail("他人のノートです。")
        }
    }

    @Test
    @DisplayName("アクセストークンが必要であること")
    fun `An access token should be needed`() = get<ErrorResponse?>("/notebooks") {
        Assert.assertEquals(StatusCode.UNAUTHORIZED_CODE, it.statusCode)
        Assert.assertNull(it.body)
    }

    private fun validate(actual: ResponseEntity<Response>, expected: Response) {
        Assert.assertEquals(StatusCode.OK_CODE, actual.statusCode)

        for (order in 1..actual.body.notebooks.size) {
            Assert.assertEquals(order, actual.body.notebooks[order - 1].order)
        }

        Assert.assertEquals(expected.notebooks.size, actual.body.notebooks.size)

        expected.notebooks.zip(actual.body.notebooks).forEach {
            Assert.assertEquals(it.first.notebookId, it.second.notebookId)
            Assert.assertEquals(it.first.title, it.second.title)
            Assert.assertEquals(it.first.order, it.first.order)
            Assert.assertEquals(it.first.createdAt, it.second.createdAt)
            Assert.assertEquals(it.first.updatedAt, it.second.updatedAt)
        }
    }
}