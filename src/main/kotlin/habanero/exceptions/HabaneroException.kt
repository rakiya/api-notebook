package habanero.exceptions

import io.jooby.StatusCode

/**
 * アプリケーション独自例外
 */
@Suppress("unused")
open class HabaneroException : RuntimeException {

    data class Error(val field: String, val descriptions: List<String>)

    data class Errors(val errors: MutableList<Error> = mutableListOf())

    val entity = Errors()

    @Transient
    var statusCode: StatusCode = StatusCode.SERVER_ERROR

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable) : super(message, cause)

    fun statusCode(statusCode: StatusCode): HabaneroException {
        this.statusCode = statusCode

        return this
    }

    fun because(vararg errors: Pair<String, List<String>>): HabaneroException {
        errors
                .map { Error(it.first, it.second) }
                .let { this.entity.errors.addAll(it) }

        return this
    }
}

/**
 * アプリケーションシステム例外
 */
@Suppress("unused")
open class HabaneroSystemException : HabaneroException {

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(statusCode: StatusCode) : super() {
        this.statusCode = statusCode
    }

}

/**
 * アプリケーションの初期化例外
 */
@Suppress("unused")
class HabaneroInitializationException : HabaneroSystemException {

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(statusCode: StatusCode) : super() {
        this.statusCode = statusCode
    }

}

/**
 * アプリケーションの責務外の例外
 */
@Suppress("unused")
class HabaneroUnexpectedException : HabaneroSystemException {

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(statusCode: StatusCode) : super() {
        this.statusCode = statusCode
    }

}

/**
 * アプリケーションのビジネス例外
 */
@Suppress("unused")
class HabaneroBusinessException : HabaneroException {

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(statusCode: StatusCode) : super() {
        this.statusCode = statusCode
    }
}
