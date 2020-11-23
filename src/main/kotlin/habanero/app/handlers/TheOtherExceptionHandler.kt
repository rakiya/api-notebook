package habanero.app.handlers

import io.jooby.Context
import io.jooby.ErrorHandler
import io.jooby.StatusCode
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pac4j.core.context.HttpConstants

/**
 * ハンドラーが未定義の全ての例外を処理するハンドラー。
 * このハンドラーで処理された例外は全てInternal Server Errorとして処理される。
 *
 * @author Ryutaro Akiya
 */
class TheOtherExceptionHandler : ErrorHandler {

    private val logger: Log = LogFactory.getLog(TheOtherExceptionHandler::class.java)!!

    override fun apply(ctx: Context, cause: Throwable, code: StatusCode) {
        logger.error(cause.stackTraceToString())

        ctx.setResponseHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.APPLICATION_JSON)
        ctx.responseCode = StatusCode.SERVER_ERROR
        ctx.render("")
    }
}
