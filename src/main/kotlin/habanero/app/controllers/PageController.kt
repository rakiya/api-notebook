package habanero.app.controllers

import habanero.app.requests.page.PageCreateRequest
import habanero.app.requests.page.PageUpdateRequest
import habanero.app.responses.page.PageBatchResponse
import habanero.app.responses.page.PageResponse
import habanero.app.services.NotebookService.Companion.getsNotebook
import habanero.app.services.PageService.Companion.addsPage
import habanero.app.services.PageService.Companion.changes
import habanero.app.services.PageService.Companion.deletesPage
import habanero.app.services.PageService.Companion.getAllPages
import habanero.app.services.PageService.Companion.getsPage
import habanero.app.services.PageService.Companion.ofPage
import habanero.app.services.PageService.Companion.toNotebook
import habanero.exceptions.*
import habanero.extensions.database.tables.Notebook
import habanero.extensions.database.tables.Page
import io.jooby.Context
import io.jooby.StatusCode
import io.jooby.annotations.*
import org.pac4j.core.profile.CommonProfile
import kotlin.reflect.KClass

/**
 * ページに関するコントローラ
 *
 * @author Ryutaro Akiya
 */
class PageController {

    @Suppress("unused")
    @GET("/notebook/{notebookId}/pages")
    fun index(
            @ContextParam user: CommonProfile,
            @PathParam notebookId: Long
    ): PageBatchResponse = try {

        // ノートを取得
        val notebook = user.getsNotebook(notebookId)
                .let { it ?: throw NotFoundException(Notebook::class, notebookId) }

        // 取得したノートにある全ページを取得
        notebook.getAllPages()
                .map { it.into(PageResponse::class.java)!! }
                .let { PageBatchResponse(it) }

    } catch (e: NotFoundException) {
        throw notFoundNotebookId(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @GET("/page/{pageId}")
    fun show(@ContextParam user: CommonProfile, @PathParam pageId: Long): PageResponse = try {

        //ページを取得
        val page = user.getsPage(pageId)
                .let { it ?: throw NotFoundException(Page::class, pageId) }

        // レスポンス
        page.into(PageResponse::class.java)

    } catch (e: NotFoundException) {
        throw notFoundPageId(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @POST("/notebook/{notebookId}/page")
    fun create(
            @ContextParam user: CommonProfile,
            @PathParam notebookId: Long,
            request: PageCreateRequest,
            ctx: Context
    ): PageResponse = try {

        // ページを追加するノートを取得
        val newPage = user.addsPage(request.title, request.order!!).toNotebook(notebookId)

        // レスポンス
        newPage.into(PageResponse::class.java)
                .also { ctx.responseCode = StatusCode.CREATED }

    } catch (e: NotFoundException) {
        throw notFoundNotebookId(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @PUT("/page/{pageId}")
    fun update(
            @ContextParam user: CommonProfile,
            @PathParam pageId: Long,
            request: PageUpdateRequest,
            ctx: Context
    ): PageResponse = try {
        //ページを更新
        val page = user.changes(request.notebookId, request.title, request.order!!).ofPage(pageId)

        // レスポンス
        page.into(PageResponse::class.java)
                .also { ctx.responseCode = StatusCode.ACCEPTED }

    } catch (e: NotFoundException) {
        throw when (e.type as KClass<*>) {
            Notebook::class -> badRequestNotebookId(e.item as Long)
            Page::class -> notFoundPageId(e.item as Long)
            else -> throw HabaneroUnexpectedException()
        }
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @DELETE("/page/{pageId}")
    fun delete(@ContextParam user: CommonProfile, @PathParam pageId: Long, ctx: Context) = try {

        // ページを削除
        user.deletesPage(pageId)
                .also { ctx.responseCode = StatusCode.NO_CONTENT }

    } catch (e: NotFoundException) {
        throw notFoundPageId(e.item as Long)
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    private fun notFoundPageId(pageId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("pageId" to listOf("ページ(ID: $pageId)は存在しません。"))
    }

    private fun notFoundNotebookId(notebookId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("notebookId" to listOf("ページ(ID: $notebookId)は存在しません。"))
    }

    private fun badRequestNotebookId(notebookId: Long): HabaneroException {
        return HabaneroBusinessException(StatusCode.BAD_REQUEST)
                .because("notebookId" to listOf("ページ(ID: $notebookId)は存在しません。"))
    }
}