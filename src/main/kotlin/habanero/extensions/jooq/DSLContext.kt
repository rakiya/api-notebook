package habanero.extensions.jooq

import org.jooq.DSLContext
import org.jooq.impl.DSL

/**
 * トランザクションを実行する
 *
 * @receiver DSLContext
 * @param transactional Function1<DSLContext, Unit>
 */
fun DSLContext.transaction(transactional: (DSLContext) -> Unit) {
    this.transaction { config ->
        val tx = DSL.using(config)
        transactional(tx)
    }
}