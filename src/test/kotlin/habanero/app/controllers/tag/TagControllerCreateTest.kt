package habanero.app.controllers.tag

import habanero.app.Application
import habanero.app.controllers.ControllerTest
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
import habanero.common.models.AccountEntity
import habanero.common.models.TagEntity
import habanero.common.models.TagEntity.Companion.createNext
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
class TagControllerCreateTest : ControllerTest() {

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
    @DisplayName("タグを作成できること")
    fun `You can create a tag`() {
        val req = Request("新しいタグ")

        validateRecordCount(TAG to 1) {
            post<Response, Request>("/tag", req, myAccount) {
                validate(req, myAccount, it)
            }
        }
    }

    @Test
    @DisplayName("他人と同じ名前のタグを作成できること")
    fun `You can create a tag which has a same name as others' tags`() {
        val req = tags
                .find { it.accountId == othersAccount.id }!!
                .let { Request(it.name) }

        validateRecordCount(TAG to 1) {
            post<Response, Request>("/tag", req, myAccount) {
                validate(req, myAccount, it)
            }
        }
    }

    @Test
    @DisplayName("同じ名前のタグを作成できないこと")
    fun `You cannot a tag which already has be inserted`() {
        val req = tags
                .find { it.accountId == myAccount.id }!!
                .let { Request(it.name) }

        validateRecordCount(TAG to 0) {
            post<ErrorResponse, Request>("/tag", req, myAccount) {
                val expected = ErrorResponse(statusCode = StatusCode.CONFLICT)
                        .add("name", listOf("既に存在します。"))

                validateFailure(expected, it)
            }
        }
    }

    @Test
    @DisplayName("リクエストにnameが含まれていること")
    fun `A request has a name attribute`() {
        val req = Request(null)

        validateRecordCount(TAG to 0) {
            post<ErrorResponse, Request>("/tag", req, myAccount) {
                val expected = ErrorResponse(statusCode = StatusCode.BAD_REQUEST)
                        .add("name", listOf("必須項目です。"))

                validateFailure(expected, it)
            }
        }
    }


    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val req = Request("新しいタグ")

        validateRecordCount(TAG to 0) {
            post<NoContentResponse, Request>("/tag", req) {
                validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
            }
        }
    }

    private fun validate(
            req: Request,
            @Suppress("SameParameterValue") account: AccountEntity,
            res: ResponseEntity<Response>
    ) {
        // リクエストreqによって登録されたタグを取得
        val inserted = db.selectFrom(TAG)
                .where(TAG.NAME.eq(req.name))
                .and(TAG.ACCOUNT_ID.eq(account.id))
                .fetchOneInto(Response::class.java)

        // ステータスコードを検証
        Assert.assertEquals(StatusCode.CREATED.value(), res.statusCode)

        // レスポンスボディを検証
        Assert.assertEquals(inserted, res.body)
    }
}