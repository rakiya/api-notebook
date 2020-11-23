package habanero.app.handlers

import io.jooby.Context
import io.jooby.ErrorHandler
import io.jooby.StatusCode

class TypeMismatchExceptionHandler : ErrorHandler {
    override fun apply(ctx: Context, cause: Throwable, code: StatusCode) {
        ctx.responseCode = StatusCode.BAD_REQUEST
        ctx.render("")
    }

}
