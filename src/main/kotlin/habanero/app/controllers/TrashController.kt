package habanero.app.controllers

import habanero.app.responses.section.SectionBatchResponse
import habanero.app.responses.section.SectionResponse
import habanero.app.services.TrashService
import habanero.app.services.TrashService.Companion.getAllTrashedSection
import habanero.app.services.TrashService.Companion.picksUpSectionFromTrash
import habanero.app.services.TrashService.Companion.throwsAwaySectionIntoTrash
import habanero.exceptions.ForbiddenException
import habanero.exceptions.HabaneroBusinessException
import habanero.exceptions.NotFoundException
import io.jooby.Context
import io.jooby.StatusCode
import io.jooby.annotations.*
import org.pac4j.core.profile.CommonProfile

/**
 * ゴミ箱内のセクションに関するコントローラ
 *
 * @author Ryutaro Akiya
 */
class TrashController {

    @Suppress("unused")
    @GET("/trash")
    fun index(@ContextParam user: CommonProfile): SectionBatchResponse {
        return user.getAllTrashedSection()
                .map { it.into(SectionResponse::class.java) }
                .let { SectionBatchResponse(it) }
    }

    @Suppress("unused")
    @POST("/trash/{sectionId}")
    fun update(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
            ctx: Context,
    ) = try {

        // ゴミ箱に入れる
        user.throwsAwaySectionIntoTrash(sectionId)
                // ステータスコードを設定
                .also { ctx.responseCode = StatusCode.ACCEPTED }

    } catch (e: NotFoundException) {
        throw HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("sectionId" to listOf("セクション(ID: ${e.item})は存在しません。"))
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @DELETE("/trash/{sectionId}")
    fun delete(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
            ctx: Context,
    ) = try {

        // ゴミ箱から取り出す
        user.picksUpSectionFromTrash(sectionId)
                // ステータスコードを設定
                .also { ctx.responseCode = StatusCode.NO_CONTENT }

    } catch (e: NotFoundException) {
        throw HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("sectionId" to listOf("セクション(ID: ${e.item})は存在しません。"))
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

}