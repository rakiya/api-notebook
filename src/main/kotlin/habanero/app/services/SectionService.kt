package habanero.app.services

import habanero.app.repositories.PageRepository
import habanero.app.repositories.Repository.Companion.of
import habanero.app.repositories.SectionRepository
import habanero.exceptions.ForbiddenException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Page
import habanero.extensions.database.tables.Section
import habanero.extensions.database.tables.records.PageRecord
import habanero.extensions.database.tables.records.SectionRecord
import habanero.extensions.jooq.transaction
import org.jooq.DSLContext
import org.jooq.types.UInteger
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.profile.CommonProfile

/**
 * @author Ryutaro Akiya
 */
class SectionService : KoinComponent {

    companion object : KoinComponent {

        data class AddSectionInstruction(val user: CommonProfile, val section: SectionRecord)

        data class UpdateSectionInstruction(val user: CommonProfile, val section: SectionRecord)

        val db: DSLContext by inject()

        /**
         * 指定のページ内の全てのセクションを取得する。
         *
         * @receiver PageRecord 指定のページ
         * @return List<SectionRecord> ページに含まれる全てのセクション
         */
        fun PageRecord.getAllSections(): List<SectionRecord> {
            return SectionRepository(db).of(this).getAll()
        }

        /**
         * セクションを取得する。
         *
         * @receiver CommonProfile セクションを取得するユーザ
         * @param sectionId Long 取得するセクションのID
         * @return SectionRecord? セクション
         */
        fun CommonProfile.getsSection(sectionId: Long): SectionRecord? {
            var section: SectionRecord? = null

            db.transaction { tx: DSLContext ->
                val repo = SectionRepository(tx)

                section = repo.get(sectionId)
                        // セクションの所有者がユーザか確認
                        .also { if (it != null && !repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
            }

            return section
        }

        /**
         * セクションを追加する命令を発行する。
         *
         * @receiver CommonProfile セクションを作成するユーザ
         * @param content String セクションの内容
         * @return AddSectionInstruction セクション追加命令
         */
        fun CommonProfile.addsSection(content: String): AddSectionInstruction {
            val record = SectionRecord().apply {
                this.content = content
            }

            return AddSectionInstruction(this, record)
        }

        /**
         * セクションを作成する。
         *
         * @receiver AddSectionInstruction セクション追加命令
         * @param pageId Long セクションを追加するページのID
         * @return SectionRecord 作成したセクション
         */
        fun AddSectionInstruction.inPageOf(pageId: Long): SectionRecord {
            lateinit var newSection: SectionRecord

            db.transaction { tx: DSLContext ->
                val pageRepo = PageRepository(tx)

                val page = pageRepo.get(pageId, forUpdate = true)
                        // ページの存在を確認
                        .let { it ?: throw NotFoundException(Page::class, pageId) }
                        // ページの所有者がユーザか確認
                        .also { if (!pageRepo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                newSection = SectionRepository(tx).of(page).of(this.section).create()
            }

            return newSection
        }

        /**
         * セクションを更新する命令を発行する。
         *
         * @receiver CommonProfile セクションを更新するユーザ
         * @param pageId Long 更新後のページID
         * @param content String 更新後の内容
         * @return UpdateSectionInstruction セクション更新命令
         */
        fun CommonProfile.changes(pageId: Long, content: String): UpdateSectionInstruction {
            val record = SectionRecord().apply {
                this.pageId = UInteger.valueOf(pageId)
                this.content = content
            }

            return UpdateSectionInstruction(this, record)
        }

        /**
         * セクションを更新する。
         *
         * @receiver UpdateSectionInstruction セクション更新命令
         * @param sectionId Long 更新するセクションのID
         * @return SectionRecord 更新後のセクション
         */
        fun UpdateSectionInstruction.ofSection(sectionId: Long): SectionRecord {
            lateinit var updatedSection: SectionRecord

            db.transaction { tx: DSLContext ->
                val repo = SectionRepository(tx)
                val pageRepo = PageRepository(tx)

                pageRepo.get(this.section.pageId.toLong(), forUpdate = true)
                        // ページが存在するか確認
                        .let { it ?: throw NotFoundException(Page::class, this.section.pageId.toLong()) }


                val section = repo.get(sectionId, forUpdate = true)
                        // セクションが存在するか確認
                        .let { it ?: throw NotFoundException(Section::class, sectionId) }
                        // 所有者を確認
                        .also { if (!repo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                updatedSection = repo.of(section).update(this.section)
            }

            return updatedSection
        }

        /**
         * セクションを削除する。
         *
         * @receiver CommonProfile セクションを削除するユーア
         * @param sectionId Long 削除するセクションのID
         */
        fun CommonProfile.deletesSection(sectionId: Long) {
            db.transaction { tx: DSLContext ->
                val repo = SectionRepository(tx)

                repo.get(sectionId, forUpdate = true)
                        // セクションが存在する確認
                        .let { it ?: throw NotFoundException(Section::class, sectionId) }
                        // セクションの所有者がユーザか確認
                        .also { if (!repo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }
                        // セクションを削除
                        .let { repo.of(it).delete() }
            }
        }

    }

}