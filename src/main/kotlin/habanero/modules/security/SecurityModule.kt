package habanero.modules.security

import habanero.modules.security.credentials.authenticators.JwtAuthenticator
import io.jooby.Jooby
import io.jooby.pac4j.Pac4jModule
import io.jooby.pac4j.Pac4jOptions
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.credentials.TokenCredentials
import org.pac4j.core.engine.DefaultSecurityLogic
import org.pac4j.core.matching.matcher.DefaultMatchers
import org.pac4j.http.client.direct.HeaderClient

/**
 * セキュリティ関連の設定
 *
 * @author Ryutaro Akiya
 */
class SecurityModule(options: Pac4jOptions, config: Config) : Pac4jModule(options, config) {
    companion object {

        private val logger: Log = LogFactory.getLog(SecurityModule::class.java)!!

        /**
         * セッション管理に関するオプション
         */
        private val options = Pac4jOptions().apply {
            // セッションの保存するか
            saveInSession = false
            // 複数形式で認証を行うか
            multiProfile = false
            // セッションを更新するか
            renewSession = false
            // ログアウトを行うか
            isLocalLogout = false
            // セッションを破壊するか
            isDestroySession = false
        }

        /**
         * 認証の形式の設定
         */
        private val config = Config().apply {
            // HTTPヘッダのAUTHORIZATIONを検証する
            // "Bearer "からは始まることを想定
            clients = Clients(
                    HeaderClient(
                            HttpConstants.AUTHORIZATION_HEADER,
                            "Bearer ",
                            JwtAuthenticator<TokenCredentials>()
                    )
            )
            addMatcher(DefaultMatchers.SECURITYHEADERS, null)
            securityLogic = DefaultSecurityLogic.INSTANCE
        }

        /**
         * Threadsafeでない(?)ため、逐次インスタンスを作成する
         *
         * @return SecurityModule セキュリティモジュールのインスタンス
         */
        fun getInstance() = SecurityModule(options, config)
    }

    /**
     * Joobyにインストールを行う。
     * ルートに"/callback"と"/logout"が自動的に追加されるため、その2つのルートを削除する
     *
     * @param application Jooby アプリケーション
     */
    override fun install(application: Jooby) {
        super.install(application)
        application.router.routes.removeIf { it.pattern == "/callback" }
        application.router.routes.removeIf { it.pattern == "/logout" }

        logger.info("Security module is installed")
    }
}