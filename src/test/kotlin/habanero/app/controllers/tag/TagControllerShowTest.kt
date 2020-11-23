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
class TagControllerShowTest : ControllerTest() {

    data class Response(
            val tagId: Long,
            val name: String,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
    )

    companion object {
        private val myAccount = AccountEntity("mine")
        private val othersAccount = AccountEntity("others")

        private val tags = runBlocking {
            mutableListOf<TagEntity>()
                    .createNext(myAccount)
                    .createNext(myAccount)
                    .createNext(othersAccount)
                    .toList()
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            db.deleteFrom(TAG).execute()
            db.batchInsert(tags.map { it.toRecord() }).execute()
        }
    }

    @Test
    @DisplayName("自分が作成したタグを取得できること")
    fun `You can get your tag`() {
        val target = getTagOwnedBy(myAccount)

        get<Response>("/tag/${target.tagId}", myAccount) {
            validate(target.toRecord().into(Response::class.java), it)
        }
    }

    @Test
    @DisplayName("他人のタグを取得できないこと")
    fun `You cannot get others' tag`() {
        val target = getTagOwnedBy(othersAccount)

        get<NoContentResponse>("/tag/${target.tagId}", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
        }
    }

    @Test
    @DisplayName("存在しないタグを取得できないこと")
    fun `You cannot get a tag not existed`() {
        get<NoContentResponse>("/tag/9999", myAccount) {
            validateNoContent(NoContentResponse(StatusCode.NOT_FOUND), it)
        }
    }

    @Test
    @DisplayName("")
    fun `You should have logged in`() {
        val target = getTagOwnedBy(myAccount)

        get<NoContentResponse>("/tag/${target.tagId}") {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
        }
    }

    /**
     * レスポンスを検証する。
     *
     * @param expected Response 期待値
     * @param actual ResponseEntity<Response> 実際の値
     */
    private fun validate(expected: Response, actual: ResponseEntity<Response>) {
        Assert.assertEquals(StatusCode.OK.value(), actual.statusCode)
        Assert.assertEquals(expected, actual.body)
    }

    /**
     * 指定したアカウントが所有するタグを取得する。
     *
     * @param owner AccountEntity 所有者
     * @param offset Int 複数件タグがある場合、何個目のタグを取るか
     * @return TagEntity タグ
     */
    private fun getTagOwnedBy(owner: AccountEntity, offset: Int = 0): TagEntity {
        return tags.filter { it.accountId == owner.id }[offset]
    }
}