package habanero.app.handlers

import io.jooby.Context
import io.jooby.ErrorHandler
import io.jooby.StatusCode
import org.pac4j.core.context.HttpConstants

/**
 * 結果が存在しない場合の例外を扱うハンドラー
 *
 * @author Ryutaro Akiya
 */
class NotFoundHandler : ErrorHandler {
    override fun apply(ctx: Context, cause: Throwable, code: StatusCode) {
        ctx.setResponseHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.APPLICATION_JSON)
        ctx.responseCode = StatusCode.NOT_FOUND
        ctx.render("")
    }
}
