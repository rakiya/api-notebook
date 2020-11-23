package habanero.app

import habanero.apis.clients.auth.AuthenticationKey
import habanero.app.controllers.*
import habanero.app.handlers.*
import habanero.exceptions.HabaneroException
import habanero.extensions.database.JooqBuilder
import habanero.extensions.gson.GsonBuilder
import habanero.modules.json.JsonModule
import habanero.modules.security.SecurityModule
import io.jooby.Kooby
import io.jooby.exception.ForbiddenException
import io.jooby.exception.MissingValueException
import io.jooby.exception.NotFoundException
import io.jooby.exception.TypeMismatchException
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.pac4j.core.exception.http.UnauthorizedAction

/**
 * アプリケーション
 *
 * @author Ryutaro Akiya
 */
class Application : Kooby({

    // == Dependency Injection ===================================================================//
    startKoin {
        module {
            single { environment }
            single { AuthenticationKey() }
            single { JooqBuilder.build(get()) }
            single { "5" }
        }.let { modules(it) }
    }

    // == Modules ================================================================================//
    // Jsonパーサ
    install(JsonModule(GsonBuilder.build()))
    // 認証
    install(SecurityModule.getInstance())


    // == Controllers ============================================================================//
    mvc(NotebookController())
    mvc(PageController())
    mvc(SectionController())
    mvc(TrashController())
    mvc(TagController())
    mvc(SectionTagController())

    // == Error Handler ==========================================================================//
    error(UnauthorizedAction::class.java, UnauthorizedHandler())
    error(ForbiddenException::class.java, ForbiddenHandler())
    error(NotFoundException::class.java, NotFoundHandler())
    error(TypeMismatchException::class.java, TypeMismatchExceptionHandler())
    error(MissingValueException::class.java, MissingValueExceptionHandler())
    error(HabaneroException::class.java, HabaneroExceptionHandler())
    error(Throwable::class.java, TheOtherExceptionHandler())
})

