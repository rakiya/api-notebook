package habanero.app.repositories

import org.jooq.DSLContext
import org.jooq.Record

/**
 * @author Ryutaro Akiya
 */
abstract class Repository<T : Record>(val tx: DSLContext) {

    /**
     * 対象のレコード
     */
    protected lateinit var target: T

    companion object {
        /**
         * 対象のレコードを指定する。
         *
         * @param target T 対象のレコード
         * @return Repository<T> リポジトリ
         */
        fun <T : Record, R : Repository<T>> R.of(target: T): R {
            this.target = target
            return this
        }
    }
}
