package habanero.app.controllers

import habanero.app.requests.section.SectionCreateRequest
import habanero.app.requests.section.SectionUpdateRequest
import habanero.app.responses.section.SectionBatchResponse
import habanero.app.responses.section.SectionResponse
import habanero.app.services.PageService.Companion.getsPage
import habanero.app.services.SectionService.Companion.addsSection
import habanero.app.services.SectionService.Companion.changes
import habanero.app.services.SectionService.Companion.deletesSection
import habanero.app.services.SectionService.Companion.getAllSections
import habanero.app.services.SectionService.Companion.getsSection
import habanero.app.services.SectionService.Companion.inPageOf
import habanero.app.services.SectionService.Companion.ofSection
import habanero.exceptions.*
import habanero.extensions.database.tables.Page
import habanero.extensions.database.tables.Section
import io.jooby.Context
import io.jooby.StatusCode
import io.jooby.annotations.*
import org.pac4j.core.profile.CommonProfile
import kotlin.reflect.KClass

/**
 * セッションに関するコントローラ
 *
 * @author Ryutaro Akiya
 */
class SectionController {

    @Suppress("unused")
    @GET("/page/{pageId}/sections")
    fun index(@ContextParam user: CommonProfile, @PathParam pageId: Long): SectionBatchResponse = try {

        val page = user.getsPage(pageId)
                // 存在するか確認
                .let { it ?: throw NotFoundException(Page::class, pageId) }

        page.getAllSections()
                .map { it.into(SectionResponse::class.java) }
                .let { SectionBatchResponse(it) }

    } catch (e: NotFoundException) {
        throw notFoundPage(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @GET("/section/{sectionId}")
    fun show(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long
    ): SectionResponse = try {

        user.getsSection(sectionId)
                // 存在を確認
                .let { it ?: throw NotFoundException(Section::class, sectionId) }
                // レスポンスに変換
                .into(SectionResponse::class.java)

    } catch (e: NotFoundException) {
        throw notFoundSection(sectionId)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @POST("/page/{pageId}/section")
    fun create(
            @ContextParam user: CommonProfile,
            @PathParam pageId: Long,
            request: SectionCreateRequest,
            ctx: Context
    ): SectionResponse = try {

        // セクションを追加
        user.addsSection(request.content).inPageOf(pageId)
                // レスポンスに変換
                .into(SectionResponse::class.java)
                // ステータスコードを設定
                .also { ctx.responseCode = StatusCode.CREATED }

    } catch (e: NotFoundException) {
        throw notFoundPage(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @PUT("/section/{sectionId}")
    fun update(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
            request: SectionUpdateRequest,
            ctx: Context
    ): SectionResponse = try {

        // セクションを更新
        user.changes(request.pageId!!, request.content).ofSection(sectionId)
                // レスポンスへ変換
                .into(SectionResponse::class.java)
                // ステータスコードを設定
                .also { ctx.responseCode = StatusCode.ACCEPTED }

    } catch (e: NotFoundException) {
        throw when(e.type as KClass<*>) {
            Page::class -> badRequestPage(e.item as Long)
            Section::class -> notFoundSection(e.item as Long)
            else -> HabaneroUnexpectedException(e)
        }
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @DELETE("/section/{sectionId}")
    fun delete(
            @ContextParam user: CommonProfile,
            @PathParam sectionId: Long,
            ctx: Context
    ) = try {

        // セクションを削除
        user.deletesSection(sectionId)
                // ステータスコードを設定
                .also { ctx.responseCode = StatusCode.NO_CONTENT }

    } catch (e: NotFoundException) {
        throw notFoundSection(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    private fun notFoundPage(pageId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("pageId" to listOf("ページ(ID: $pageId)は存在しません。"))
    }

    private fun notFoundSection(sectionId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("sectionId" to listOf("セクション(ID: $sectionId)は存在しません。"))
    }

    private fun badRequestPage(pageId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.BAD_REQUEST)
                .because("pageId" to listOf("ページ(ID: $pageId)は存在しません。"))
    }
}