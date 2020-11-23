package habanero.app.controllers

import habanero.app.responses.sectionTagDetail.SectionTagDetailBatchResponse
import habanero.app.responses.sectionTagDetail.SectionTagDetailResponse
import habanero.app.services.SectionService
import habanero.app.services.SectionTagService.Companion.fromSection
import habanero.app.services.SectionTagService.Companion.getAllTagsOfSection
import habanero.app.services.SectionTagService.Companion.onSection
import habanero.app.services.SectionTagService.Companion.putTag
import habanero.app.services.SectionTagService.Companion.takeOffTag
import habanero.app.services.TagService
import habanero.exceptions.*
import habanero.extensions.database.tables.Section
import habanero.extensions.database.tables.Tag
import io.jooby.Context
import io.jooby.StatusCode
import io.jooby.annotations.*
import org.pac4j.core.profile.CommonProfile
import kotlin.reflect.KClass

/**
 * セクションのタグに関するコントローラ
 */
class SectionTagController {

    @Suppress("unused")
    @GET("/section/{sectionId}/tags")
    fun index(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
    ): SectionTagDetailBatchResponse = try {
        user.getAllTagsOfSection(sectionId)
                .groupBy { it.sectionId }
                .map { (sectionId, tagDetails) ->
                    val tags = tagDetails.map {
                        it.into(SectionTagDetailResponse.TagDetail::class.java)
                    }

                    SectionTagDetailResponse(sectionId.toLong(), tags)
                }
                .let { SectionTagDetailBatchResponse(it) }
    } catch (e: NotFoundException) {
        throw notFoundSection(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @POST("/section/{sectionId}/tag/{tagId}")
    fun create(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
            @PathParam tagId: Long,
            ctx: Context,
    ) = try {
        user.putTag(tagId).onSection(sectionId)
                .also { ctx.responseCode = StatusCode.CREATED }
    } catch (e: NotFoundException) {
        when (e.type as KClass<*>) {
            Section::class -> throw notFoundSection(e.item as Long)
            Tag::class -> throw notFoundTag(e.item as Long)
            else -> throw HabaneroUnexpectedException(e)
        }
    } catch (e: DuplicateException) {
        throw HabaneroBusinessException(StatusCode.CONFLICT)
                .because("tag" to
                        listOf("タグ(ID: ${tagId})は既にセクション(ID: $sectionId)にタグ付けされています。"))
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @DELETE("/section/{sectionId}/tag/{tagId}")
    fun delete(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
            @PathParam tagId: Long,
            ctx: Context,
    ) = try {
        user.takeOffTag(tagId).fromSection(sectionId)
                .also { ctx.responseCode = StatusCode.NO_CONTENT }
    } catch (e: NotFoundException) {
        when (e.type as KClass<*>) {
            Section::class -> throw notFoundSection(e.item as Long)
            Tag::class -> throw notFoundTag(e.item as Long)
            else -> throw HabaneroUnexpectedException(e)
        }
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    private fun notFoundSection(sectionId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("sectionId" to listOf("セクション(ID: $sectionId)は存在しません。"))
    }

    private fun notFoundTag(tagId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("tagId" to listOf("タグ(ID: $tagId)は存在しません。"))
    }
}