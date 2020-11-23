package habanero.app.services

import habanero.app.repositories.Repository.Companion.of
import habanero.app.repositories.TagRepository
import habanero.exceptions.ForbiddenException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Tag
import habanero.extensions.database.tables.records.TagRecord
import habanero.extensions.jooq.transaction
import org.jooq.DSLContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.profile.CommonProfile

class TagService : KoinComponent {

    companion object : KoinComponent {

        data class UpdateTagInstruction(val user: CommonProfile, val tag: TagRecord)

        val db: DSLContext by inject()

        /**
         * ユーザが作成したタグを全て取得する。
         *
         * @receiver CommonProfile ユーザ
         * @return List<TagRecord> タグ
         */
        fun CommonProfile.getAllTags(): List<TagRecord> {
            return TagRepository(db).getAllOwnedBy(this.id)
        }

        /**
         * 指定されたIDを持つタグを取得する
         *
         * @receiver CommonProfile ユーザ
         * @param tagId Long 取得したいタグのID
         * @return TagRecord? タグ
         */
        fun CommonProfile.getTag(tagId: Long): TagRecord? {
            var tag: TagRecord? = null

            db.transaction { tx: DSLContext ->
                val repo = TagRepository(tx)

                tag = repo.get(tagId)
                        // タグの所有者を確認
                        .also { if (it != null && !repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
            }

            return tag
        }

        /**
         * ユーザがタグを作成する。
         *
         * @receiver CommonProfile タグを作成するユーザ
         * @param name String タグの名前
         * @return TagRecord 作成されたタグ
         */
        fun CommonProfile.createTag(name: String): TagRecord {
            // ユーザ
            val user = this
            //新しいタグ
            lateinit var newTag: TagRecord

            db.transaction { tx: DSLContext ->
                val repo = TagRepository(tx)

                val record = TagRecord().apply {
                    this.name = name
                    this.accountId = user.id
                }

                newTag = repo.of(record).create()
            }

            return newTag
        }

        /**
         * タグを更新する命令を発行する。
         *
         * @receiver CommonProfile タグを更新するユーザ
         * @param name String タグの名前
         * @return UpdateTagInstruction タグ更新命令
         */
        fun CommonProfile.change(name: String): UpdateTagInstruction {
            val record = TagRecord().apply { this.name = name }

            return UpdateTagInstruction(this, record)
        }

        /**
         * 指定されたIDを持つタグを更新する。
         *
         * @receiver UpdateTagInstruction タグ更新命令
         * @param tagId Long 更新するタグのID
         * @return TagRecord 更新後のタグ
         */
        fun UpdateTagInstruction.ofTag(tagId: Long): TagRecord {
            lateinit var tag: TagRecord

            db.transaction { tx: DSLContext ->
                val repo = TagRepository(tx)

                val target = repo.get(tagId)
                        // 登録済みか確認
                        .let { it ?: throw NotFoundException(Tag::class, tagId) }
                        // 所有者を確認
                        .also { if (!repo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                tag = repo.of(target).update(this.tag)
            }

            return tag
        }

        fun CommonProfile.deleteTag(tagId: Long) = db.transaction { tx: DSLContext ->
            val repo = TagRepository(tx)

            val target = repo.get(tagId)
                    // タグの存在を確認
                    .let { it ?: throw NotFoundException(Tag::class, tagId) }
                    // タグの所有者を確認
                    .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }

            repo.of(target).delete()
        }
    }

}