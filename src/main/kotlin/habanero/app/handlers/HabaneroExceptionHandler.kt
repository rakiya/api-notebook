package habanero.app.handlers

import habanero.exceptions.HabaneroException
import io.jooby.Context
import io.jooby.ErrorHandler
import io.jooby.StatusCode

/**
 * アプリケーション独自の例外を扱うハンドラー
 *
 * @author Ryutaro Akiya
 */
class HabaneroExceptionHandler : ErrorHandler {
    override fun apply(ctx: Context, cause: Throwable, code: StatusCode) {
        (cause as HabaneroException)
                .let {
                    ctx.responseCode = it.statusCode
                    ctx.render(it.entity)
                }
    }
}