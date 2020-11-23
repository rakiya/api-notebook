package habanero.app.controllers.tag

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.common.models.TagEntity
import habanero.common.models.TagEntity.Companion.createNext
import habanero.extensions.database.tables.Tag
import habanero.extensions.database.tables.Tag.TAG
import io.jooby.JoobyTest
import io.jooby.StatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@JoobyTest(Application::class)
class TagControllerUpdateTest : ControllerTest() {

    data class Request(
            val name: String?
    )

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
    }

    @BeforeEach
    fun beforeEach() {
        db.deleteFrom(TAG).execute()
        db.batchInsert(tags.map { it.toRecord() }).execute()
    }

    @Test
    @DisplayName("タグの内容を更新できること")
    fun `You can update a tag`() {
        val target = getTagOwnedBy(myAccount)
        val req = Request("変更されたタグ")

        put<Response, Request>("/tag/${target.tagId}", req, myAccount) {
            validate(req, myAccount, it)
        }
    }

    @Test
    @DisplayName("他人と同じ名前のタグに変更できること")
    fun `You can update a tag to one which has a same name as others' tags`() {
        val target = getTagOwnedBy(myAccount)
        val req = Request(getTagOwnedBy(othersAccount).name)

        put<Response, Request>("/tag/${target.tagId}", req, myAccount) {
            validate(req, myAccount, it)
        }
    }

    @Test
    @DisplayName("同じ名前のタグに変更できないこと")
    fun `You cannot update a tag to one which already has be inserted`() {
        val target = getTagOwnedBy(myAccount)
        val req = Request(getTagOwnedBy(myAccount, offset = 1).name)

        put<ErrorResponse, Request>("/tag/${target.tagId}", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.CONFLICT)
                    .add("name", listOf("既に存在します。"))

            validateFailure(expected, it)
        }
    }

    @Test
    @DisplayName("リクエストにnameが含まれていること")
    fun `A request has a name attribute`() {
        val target = getTagOwnedBy(myAccount)
        val req = Request(null)

        put<ErrorResponse, Request>("/tag/${target.tagId}", req, myAccount) {
            val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                    .add("name", listOf("必須項目です。"))

            validateFailure(expected, it)
        }
    }


    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val target = getTagOwnedBy(myAccount)
        val req = Request("変更されたタグ")

        put<NoContentResponse, Request>("/tag/${target.tagId}", req) {
            validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
        }
    }

    private fun validate(
            req: Request,
            @Suppress("SameParameterValue") account: AccountEntity,
            res: ResponseEntity<Response>
    ) {
        // リクエストreqによって登録されたタグを取得
        val inserted = db.selectFrom(Tag.TAG)
                .where(Tag.TAG.NAME.eq(req.name))
                .and(Tag.TAG.ACCOUNT_ID.eq(account.id))
                .fetchOneInto(Response::class.java)

        // ステータスコードを検証
        Assert.assertEquals(StatusCode.ACCEPTED.value(), res.statusCode)

        // レスポンスボディを検証
        Assert.assertEquals(inserted, res.body)
    }

    private fun getTagOwnedBy(owner: AccountEntity, offset: Int = 0): TagEntity {
        return tags.filter { it.accountId == owner.id }[offset]
    }
}