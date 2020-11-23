package habanero.app.services

import habanero.app.repositories.Repository.Companion.of
import habanero.app.repositories.SectionRepository
import habanero.exceptions.ForbiddenException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Section
import habanero.extensions.database.tables.records.SectionRecord
import habanero.extensions.jooq.transaction
import org.jooq.DSLContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.profile.CommonProfile

/**
 * ゴミ箱にあるセクションの関する操作
 *
 * @author Ryutaro Akiya
 */
class TrashService : KoinComponent {

    companion object : KoinComponent {

        val db: DSLContext by inject()

        /**
         * ゴミ箱内のセクションを全て取得する。
         *
         * @receiver CommonProfile セクションを取得するユーザ
         * @return List<SectionRecord> ゴミ箱内のセクション
         */
        fun CommonProfile.getAllTrashedSection(): List<SectionRecord> {
            return SectionRepository(db).getAllTrashedOwnedBy(this.id)
        }

        fun CommonProfile.throwsAwaySectionIntoTrash(sectionId: Long) = db.transaction { tx: DSLContext ->
            val repo = SectionRepository(tx)

            val record = SectionRecord().apply { isTrashed = 1 }

            repo.get(sectionId)
                    // セクションの存在を確認
                    .let { it ?: throw NotFoundException(Section::class, sectionId) }
                    // セクションの所有者を確認
                    .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
                    // ゴミ箱に捨てる
                    .let { repo.of(it).update(record) }
        }

        fun CommonProfile.picksUpSectionFromTrash(sectionId: Long) = db.transaction { tx: DSLContext ->
            val repo = SectionRepository(tx)

            val record = SectionRecord().apply { isTrashed = 0 }

            repo.get(sectionId)
                    // セクションの存在を確認
                    .let { it ?: throw NotFoundException(Section::class, sectionId) }
                    // セクションの所有者を確認
                    .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
                    // ゴミ箱から取り出す
                    .let { repo.of(it).update(record) }

        }
    }

}