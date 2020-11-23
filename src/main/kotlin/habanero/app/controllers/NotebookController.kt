package habanero.app.controllers

import habanero.app.requests.notebook.CreateRequest
import habanero.app.requests.notebook.UpdateRequest
import habanero.app.responses.notebook.NotebookBatchResponse
import habanero.app.responses.notebook.NotebookResponse
import habanero.app.services.NotebookService
import habanero.app.services.NotebookService.Companion.createsNotebook
import habanero.app.services.NotebookService.Companion.deletesNotebook
import habanero.app.services.NotebookService.Companion.getsNotebook
import habanero.app.services.NotebookService.Companion.updatesNotebook
import habanero.exceptions.DuplicateException
import habanero.exceptions.ForbiddenException
import habanero.exceptions.HabaneroBusinessException
import habanero.exceptions.NotFoundException
import habanero.extensions.database.tables.Notebook
import io.jooby.Context
import io.jooby.StatusCode
import io.jooby.annotations.*
import org.pac4j.core.profile.CommonProfile

/**
 * ノートのコントローラ
 *
 * @author Ryutaro Akiya
 */
class NotebookController {

    private val notebookService = NotebookService()

    @Suppress("unused")
    @GET("/notebooks")
    fun index(@ContextParam user: CommonProfile): NotebookBatchResponse {
        // 全てのノートを取得
        val notebooks = notebookService.getAllOwnedBy(user)

        // レスポンス
        return notebooks
                .map { it.into(NotebookResponse::class.java) }
                .let { NotebookBatchResponse(it) }
    }

    @Suppress("unused")
    @GET("/notebook/{notebookId}")
    fun show(@ContextParam user: CommonProfile, @PathParam notebookId: Long): NotebookResponse = try {

        // ノートを取得
        user.getsNotebook(notebookId)
                // ノートの存在を確認
                .let { it ?: throw NotFoundException(Notebook::class, notebookId) }
                // レスポンスに変換
                .into(NotebookResponse::class.java)


    } catch (e: NotFoundException) {
        throw HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("notebookId" to listOf("ノート(ID: ${e.item})が存在しません。"))
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @POST("/notebook")
    fun create(
            @ContextParam user: CommonProfile,
            request: CreateRequest,
            ctx: Context
    ): NotebookResponse = try {

        // ノートを作成
        val newNotebook = user.createsNotebook(request.title)

        // レスポンス
        newNotebook.into(NotebookResponse::class.java)
                .also { ctx.responseCode = StatusCode.CREATED }

    } catch (e: DuplicateException) {
        throw HabaneroBusinessException(StatusCode.CONFLICT)
                .because("title" to listOf("既に存在します。"))
    }

    @Suppress("unused")
    @PUT("/notebook/{notebookId}")
    fun update(
            @ContextParam user: CommonProfile,
            @PathParam notebookId: Long,
            request: UpdateRequest,
            ctx: Context
    ): NotebookResponse = try {
        // 更新
        val updatedNotebook = user.updatesNotebook(notebookId, request.title, request.order!!)

        updatedNotebook.into(NotebookResponse::class.java)
                .also { ctx.responseCode = StatusCode.ACCEPTED }

    } catch (e: DuplicateException) {
        throw HabaneroBusinessException(StatusCode.CONFLICT)
                .because("title" to listOf("既に存在します。"))
    } catch (e: NotFoundException) {
        throw HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("notebookId" to listOf("ノート(ID: ${e.item})は存在しません。"))
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }

    @Suppress("unused")
    @DELETE("/notebook/{notebookId}")
    fun delete(
            @ContextParam user: CommonProfile,
            @PathParam notebookId: Long,
            ctx: Context
    ) = try {
        // ノートを削除
        user.deletesNotebook(notebookId)
                .also { ctx.responseCode = StatusCode.NO_CONTENT }
    } catch (e: NotFoundException) {
        throw HabaneroBusinessException(StatusCode.NOT_FOUND)
                .because("notebookId" to listOf("ノート(ID: ${e.item})が存在しません。"))
    } catch (e: ForbiddenException) {
        throw io.jooby.exception.ForbiddenException()
    }
}
