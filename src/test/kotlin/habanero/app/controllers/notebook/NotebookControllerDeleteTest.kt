package habanero.app.controllers.notebook

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
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

@JoobyTest(Application::class)
class NotebookControllerDeleteTest : ControllerTest() {

    companion object {
        private val myAccount = AccountEntity("user1")
        private val othersAccount = AccountEntity("user2")

        private val testNotebookData = {
            listOf(
                    NotebookRecord().apply {
                        notebookId = UInteger.valueOf(1)
                        accountId = myAccount.id
                        title = "私のノート"
                        order = UInteger.valueOf(1)
                    },
                    NotebookRecord().apply {
                        notebookId = UInteger.valueOf(2)
                        accountId = othersAccount.id
                        title = "他人のノート"
                        order = UInteger.valueOf(1)
                    },
                    NotebookRecord().apply {
                        notebookId = UInteger.valueOf(3)
                        accountId = myAccount.id
                        title = "私のノート2"
                        order = UInteger.valueOf(2)
                    }
            )
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(NOTEBOOK).execute()
            db.batchInsert(testNotebookData()).execute()
        }
    }


    @Test
    @DisplayName("ノートを削除できること")
    fun `You can delete your notebook`() {
        val myNotebook = testNotebookData().first { it.accountId == myAccount.id }

        delete<NoContentResponse>("/notebook/${myNotebook.notebookId}", myAccount) {
            validateResponse(NoContentResponse(StatusCode.NO_CONTENT), it)
            validateRecord(myNotebook.notebookId.toLong())
        }
    }

    @Test
    @DisplayName("他人のノートを削除できないこと")
    fun `You cannot delete others' notebook`() {
        val othersNotebook = testNotebookData().first { it.accountId == othersAccount.id }

        delete<NoContentResponse>("/notebook/${othersNotebook.notebookId}", myAccount) {
            validateResponse(NoContentResponse(StatusCode.FORBIDDEN), it)
        }
    }

    @Test
    @DisplayName("存在しないノートを削除できないこと")
    fun `You cannot delete a notebook not existed`() = delete<ErrorResponse>("/notebook/9999", myAccount) {
        val expected = ErrorResponse(statusCode = StatusCode.NOT_FOUND)
                .add("notebookId", listOf("ノート(ID: 9999)が存在しません。"))
        validateFailure(expected, it)
    }

    private fun validateResponse(expected: NoContentResponse, actual: ResponseEntity<NoContentResponse>) {
        Assert.assertEquals(expected.statusCode!!.value(), actual.statusCode)
    }

    private fun validateRecord(notebookId: Long) {
        val notebook = db.selectFrom(NOTEBOOK)
                .where(NOTEBOOK.NOTEBOOK_ID.eq(UInteger.valueOf(notebookId)))
                .fetchOne()

        Assert.assertNull(notebook)
    }
}