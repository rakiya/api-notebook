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
import org.jooq.types.UInteger
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


@JoobyTest(Application::class)
class TagControllerDeleteTest : ControllerTest() {

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
    @DisplayName("自分が作成したタグを削除できること")
    fun `You can delete your tag`() {
        val target = getTagOwnedBy(myAccount)

        validateRecordCount(TAG to -1) {
            delete<NoContentResponse>("/tag/${target.tagId}", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.NO_CONTENT), it)
                validate(target)
            }
        }
    }

    @Test
    @DisplayName("他人の作成したタグを削除できないこと")
    fun `You cannot delete others' tag`() {
        val target = getTagOwnedBy(othersAccount)

        validateRecordCount(TAG to 0) {
            delete<NoContentResponse>("/tag/${target.tagId}", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.FORBIDDEN), it)
            }
        }
    }

    @Test
    @DisplayName("存在しないタグを削除できないこと")
    fun `You cannot delete tag not existed`() {

        validateRecordCount(TAG to 0) {
            delete<NoContentResponse>("/tag/9999", myAccount) {
                validateNoContent(NoContentResponse(StatusCode.NOT_FOUND), it)
            }
        }
    }

    @Test
    @DisplayName("ログインしなければならないこと")
    fun `You should have logged in`() {
        val target = getTagOwnedBy(othersAccount)

        validateRecordCount(TAG to 0) {
            delete<NoContentResponse>("/tag/${target.tagId}") {
                validateNoContent(NoContentResponse(StatusCode.UNAUTHORIZED), it)
            }
        }
    }


    private fun validate(target: TagEntity) {
        val record = db.selectFrom(TAG)
                .where(TAG.TAG_ID.eq(UInteger.valueOf(target.tagId)))
                .fetchOne()

        Assert.assertNull(record)
    }

    private fun getTagOwnedBy(owner: AccountEntity, offset: Int = 0): TagEntity {
        return tags.filter { it.accountId == owner.id }[offset]
    }
}