package habanero.extensions.database

import habanero.exceptions.HabaneroInitializationException
import io.jooby.Environment
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.DriverManager

/**
 * データベースコネクションを取得
 *
 * @author Ryutaro Akiya
 */
object JooqBuilder {
    fun build(env: Environment): DSLContext {
        val url = env.config.getString("db.url")!!
        val username = env.config.getString("db.username")!!
        val password = env.config.getString("db.password")!!

        kotlin.runCatching {
            DriverManager.getConnection(url, username, password)
        }.onSuccess {
            return@build DSL.using(it, SQLDialect.MYSQL)
        }

        throw HabaneroInitializationException("データベースのコネクションを作成できません")
    }
}