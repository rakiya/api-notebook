package habanero.app.repositories

import habanero.exceptions.DuplicateException
import habanero.exceptions.HabaneroUnexpectedException
import habanero.extensions.database.tables.Tag.TAG
import habanero.extensions.database.tables.records.TagRecord
import habanero.extensions.jooq.forUpdateIf
import io.jooby.StatusCode
import org.jooq.DSLContext
import org.jooq.types.UInteger
import java.sql.SQLIntegrityConstraintViolationException

/**
 * タグのリポジトリ
 *
 * @author Ryutaro Akiya
 */
class TagRepository(tx: DSLContext) : Repository<TagRecord>(tx) {

    /**
     * 指定されたアカウントが所有する、全てのタグを取得する。
     *
     * @param ownerId String 所有者のアカウントID
     * @return List<TagRecord> タグ
     */
    fun getAllOwnedBy(ownerId: String): List<TagRecord> {
        return tx.selectFrom(TAG)
                .where(TAG.ACCOUNT_ID.eq(ownerId))
                .fetchInto(TAG)
    }

    /**
     * 指定されたIDのタグを取得する。
     *
     * @param tagId Long タグID
     * @return TagRecord タグ
     */
    fun get(tagId: Long, forUpdate: Boolean = false): TagRecord? {
        return tx.selectFrom(TAG)
                .where(TAG.TAG_ID.eq(UInteger.valueOf(tagId)))
                .forUpdateIf(forUpdate)
                .fetchOneInto(TAG)
    }

    /**
     * タグを作成する。
     *
     * @return TagRecord 作成したタグ
     */
    fun create(): TagRecord {
        lateinit var newTag: TagRecord

        kotlin.runCatching {
            tx.insertInto(TAG)
                    .set(TAG.ACCOUNT_ID, target.accountId)
                    .set(TAG.NAME, target.name)
                    .returningResult(TAG.TAG_ID)
                    .fetchOne()
                    .component1()
        }.onSuccess {
            newTag = tx.selectFrom(TAG)
                    .where(TAG.TAG_ID.eq(it))
                    .fetchOneInto(TAG)
        }.onFailure {
            when (it.cause) {
                is SQLIntegrityConstraintViolationException -> throw DuplicateException()
                else -> throw HabaneroUnexpectedException(it).statusCode(StatusCode.SERVER_ERROR)

            }
        }

        return newTag
    }

    /**
     * タグの内容を更新する。
     *
     * @receiver TagRecord 更新するタグ
     * @param record TagRecord 更新内容
     * @return TagRecord 更新されたタグ
     */
    fun update(record: TagRecord): TagRecord {
        lateinit var updatedTag: TagRecord

        kotlin.runCatching {
            tx.update(TAG)
                    .set(TAG.NAME, record.name)
                    .where(TAG.TAG_ID.eq(target.tagId))
                    .execute()
        }.onSuccess {
            updatedTag = tx.selectFrom(TAG)
                    .where(TAG.TAG_ID.eq(target.tagId))
                    .fetchOneInto(TAG)
        }.onFailure {
            when (it.cause) {
                is SQLIntegrityConstraintViolationException -> throw DuplicateException()
                else -> throw HabaneroUnexpectedException(it)
            }
        }

        return updatedTag
    }

    /**
     * タグを削除する。
     */
    fun delete() {
        tx.deleteFrom(TAG)
                .where(TAG.TAG_ID.eq(target.tagId))
                .execute()
    }

    /**
     * IDがaccountIdのアカウントがタグを所有しているか判定する。
     *
     * @param accountId String アカウントID
     * @return Boolean 所有している場合はtrue、していない場合はfalse
     */
    fun isOwnedBy(accountId: String): Boolean {
        val actualOwnerId: String? = tx.select(TAG.ACCOUNT_ID)
                .from(TAG)
                .where(TAG.TAG_ID.eq(target.tagId))
                .fetchOneInto(String::class.java)

        return actualOwnerId != null && accountId == actualOwnerId
    }
}