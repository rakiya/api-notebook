package habanero.app.controllers.tag

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.common.models.TagEntity
import habanero.common.models.TagEntity.Companion.createNext
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
class TagControllerIndexTest : ControllerTest() {

    data class Response(val tags: List<ResponseInner>) {
        data class ResponseInner(
                val tagId: Long,
                val name: String,
                val createdAt: LocalDateTime,
                val updatedAt: LocalDateTime
        )
    }

    companion object {
        val myAccount = AccountEntity("mine")
        val othersAccount = AccountEntity("others")

        val tags = runBlocking {
            mutableListOf<TagEntity>()
                    .createNext(myAccount)
                    .createNext(myAccount)
                    .createNext(othersAccount)
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(TAG).execute()
            db.batchInsert(tags.map { it.toRecord() }).execute()
        }
    }

    @Test
    @DisplayName("自分が作成したタグを全て取得できること")
    fun `You can get all your tags`() {
        get<Response>("/tags", myAccount) { actual ->
            val expected = tags.filter { it.accountId == myAccount.id }
                    .map { it.toRecord().into(Response.ResponseInner::class.java) }
                    .let { Response(it) }

            validate(expected, actual)
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        get<NoContentResponse>("/tags") {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
        }
    }

    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)

        Assert.assertEquals(expected, actual.body)
    }
}