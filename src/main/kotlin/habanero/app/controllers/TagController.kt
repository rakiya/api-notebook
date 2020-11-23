package habanero.app.controllers

import habanero.app.requests.tag.TagRequest
import habanero.app.responses.tag.TagBatchResponse
import habanero.app.responses.tag.TagResponse
import habanero.app.services.TagService.Companion.change
import habanero.app.services.TagService.Companion.createTag
import habanero.app.services.TagService.Companion.deleteTag
import habanero.app.services.TagService.Companion.getAllTags
import habanero.app.services.TagService.Companion.getTag
import habanero.app.services.TagService.Companion.ofTag
import habanero.exceptions.*
import habanero.extensions.database.tables.Tag
import io.jooby.Context
import io.jooby.StatusCode
import io.jooby.annotations.*
import org.pac4j.core.profile.CommonProfile

/**
 * タグに関するコントローラ
 *
 * @author Ryutaro Akiya
 */
class TagController {

    @Suppress("unused")
    @GET("/tags")
    fun index(@ContextParam user: CommonProfile): TagBatchResponse {
        return user.getAllTags()
                .map { it.into(TagResponse::class.java) }
                .let { TagBatchResponse(it) }
    }

    @Suppress("unused")
    @GET("/tag/{tagId}")
    fun show(@PathParam tagId: Long, @ContextParam user: CommonProfile): TagResponse = try {
        user.getTag(tagId)
                .let { it ?: throw NotFoundException(Tag::class, tagId) }
                .into(TagResponse::class.java)
    } catch (e: NotFoundException) {
        throw notFoundTag(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @POST("/tag")
    fun create(
            @ContextParam user: CommonProfile,
            request: TagRequest,
            ctx: Context,
    ): TagResponse = try {
        user.createTag(request.name)
                .into(TagResponse::class.java)
                .also { ctx.responseCode = StatusCode.CREATED }
    } catch (e: DuplicateException) {
        throw conflictTag()
    }

    @Suppress("unused")
    @PUT("/tag/{tagId}")
    fun update(
            @ContextParam user: CommonProfile,
            @PathParam tagId: Long,
            request: TagRequest,
            ctx: Context,
    ): TagResponse = try {
        user.change(request.name).ofTag(tagId)
                .into(TagResponse::class.java)
                .also { ctx.responseCode = StatusCode.ACCEPTED }
    } catch (e: DuplicateException) {
        throw conflictTag()
    } catch (e: NotFoundException) {
        throw notFoundTag(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @DELETE("/tag/{tagId}")
    fun delete(
            @ContextParam user: CommonProfile,
            @PathParam tagId: Long,
            ctx: Context,
    ) = try {
        user.deleteTag(tagId)
                .also { ctx.responseCode = StatusCode.NO_CONTENT }
    } catch (e: NotFoundException) {
        throw notFoundTag(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }


    private fun notFoundTag(tagId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("tagId" to listOf("タグ(ID: $tagId)は存在しません。"))
    }

    private fun conflictTag(): HabaneroException {
        return HabaneroBusinessException(StatusCode.CONFLICT)
                .because("name" to listOf("既に存在します。"))
    }
}