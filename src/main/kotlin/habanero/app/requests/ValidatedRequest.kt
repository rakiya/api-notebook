package habanero.app.requests

import habanero.exceptions.HabaneroBusinessException
import habanero.exceptions.HabaneroException
import io.jooby.StatusCode
import javax.validation.ConstraintViolation

/**
 * バリデーションするリクエスト
 *
 * @author Ryutaro Akiya
 */
interface ValidatedRequest {

    /**
     * Hibernate Validationの例外をアプリケーション独自例外に変換する。
     *
     * @receiver Set<ConstraintViolation<T>>
     * @return HabaneroException
     */
    fun <T> Set<ConstraintViolation<T>>.toException(): HabaneroException {
        val errors = mutableMapOf<String, MutableList<String>>()

        this.forEach {
            errors.putIfAbsent(it.propertyPath.toString(), mutableListOf())
            errors[it.propertyPath.toString()]!!.add(it.message)
        }

        errors.map { it.key to it.value }
                .toTypedArray()
                .let {
                    return HabaneroBusinessException()
                            .statusCode(StatusCode.BAD_REQUEST)
                            .because(*it)
                }
    }

}
