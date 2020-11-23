package habanero.app.services

import habanero.app.repositories.Repository.Companion.of
import habanero.app.repositories.SectionRepository
import habanero.app.repositories.SectionTagRepository
import habanero.app.repositories.TagRepository
import habanero.exceptions.ForbiddenException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Section
import habanero.extensions.database.tables.Tag
import habanero.extensions.database.tables.records.SectionTagDetailRecord
import habanero.extensions.jooq.transaction
import org.jooq.DSLContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.profile.CommonProfile

class SectionTagService : KoinComponent {

    companion object : KoinComponent {

        data class PutOnTagInstruction(val user: CommonProfile, val tagId: Long)

        data class TakeOffTagInstruction(val user: CommonProfile, val tagId: Long)

        val db: DSLContext by inject()

        fun CommonProfile.getAllTagsOfSection(sectionId: Long): List<SectionTagDetailRecord> {
            lateinit var sectionTags: List<SectionTagDetailRecord>
            db.transaction { tx: DSLContext ->
                val sectionRepo = SectionRepository(tx)

                sectionRepo.get(sectionId, forUpdate = true)
                        // セクションが存在するか確認
                        .let { it ?: throw NotFoundException(Section::class, sectionId) }
                        // セクションの所有者を確認
                        .also { if (!sectionRepo.of(it).isOwnedBy(this.id)) throw ForbiddenException() }

                sectionTags = SectionTagRepository(tx).getAllTagsOf(sectionId)
            }

            return sectionTags
        }

        fun CommonProfile.putTag(tagId: Long): PutOnTagInstruction {
            return PutOnTagInstruction(this, tagId)
        }

        fun PutOnTagInstruction.onSection(sectionId: Long) {
            db.transaction { tx: DSLContext ->
                val sectionRepo = SectionRepository(tx)
                val tagRepo = TagRepository(tx)
                val repo = SectionTagRepository(tx)

                val section = sectionRepo.get(sectionId, forUpdate = true)
                        .let { it ?: throw NotFoundException(Section::class, sectionId) }
                        .also { if (!sectionRepo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                val tag = tagRepo.get(tagId, forUpdate = true)
                        .let { it ?: throw NotFoundException(Tag::class, this.tagId) }
                        .also { if (!tagRepo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                repo.of(tag).createOn(section.sectionId.toLong())
            }
        }

        fun CommonProfile.takeOffTag(tagId: Long): TakeOffTagInstruction {
            return TakeOffTagInstruction(this, tagId)
        }

        fun TakeOffTagInstruction.fromSection(sectionId: Long) {
            db.transaction { tx: DSLContext ->
                val sectionRepo = SectionRepository(tx)
                val tagRepo = TagRepository(tx)
                val repo = SectionTagRepository(tx)

                val section = sectionRepo.get(sectionId, forUpdate = true)
                        .let { it ?: throw NotFoundException(Section::class, sectionId) }
                        .also { if (!sectionRepo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                val tag = tagRepo.get(tagId, forUpdate = true)
                        .let { it ?: throw NotFoundException(Tag::class, this.tagId) }
                        .also { if (!tagRepo.of(it).isOwnedBy(this.user.id)) throw ForbiddenException() }

                repo.of(tag).deleteFrom(section.sectionId.toLong())

            }
        }
    }
}