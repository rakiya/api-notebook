package habanero.app.handlers

import io.jooby.Context
import io.jooby.ErrorHandler
import io.jooby.StatusCode

/**
 * HTTPヘッダに足りない要素があった場合の例外を扱うハンドラー。
 * 例えばContent-Typeがない場合などは、この例外が発生する。
 *
 * @author Ryutaro Akiya
 */
class MissingValueExceptionHandler : ErrorHandler {
    override fun apply(ctx: Context, cause: Throwable, code: StatusCode) {
        ctx.responseCode = StatusCode.BAD_REQUEST
        ctx.render("")
    }
}