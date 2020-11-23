package habanero.extensions.jooq

import org.jooq.Record
import org.jooq.SelectForUpdateStep
import org.jooq.SelectOptionStep

fun <R : Record> SelectForUpdateStep<R>.forUpdateIf(isNeeded: Boolean): SelectOptionStep<R> {
    return if (isNeeded) {
        this.forUpdate()
    } else {
        this
    }
}