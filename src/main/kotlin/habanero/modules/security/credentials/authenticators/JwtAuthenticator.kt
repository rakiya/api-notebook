package habanero.modules.security.credentials.authenticators

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import habanero.apis.clients.auth.AuthenticationKey
import org.apache.commons.logging.LogFactory
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.TokenCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.profile.CommonProfile

/**
 * JWTアクセスキーの検証を行う。
 *
 * @author Ryutaro Akiya
 *
 * @param C : TokenCredentials 認証オブジェクトの形式
 * @property authKey AuthenticationKey アクセストークンを検証するための鍵
 */
class JwtAuthenticator<C : TokenCredentials> : Authenticator<C>, KoinComponent {

    private val authKey: AuthenticationKey by inject()

    private val logger = LogFactory.getLog(this::class.java)

    /**
     * アクセスキーの検証を行う。
     */
    override fun validate(credentials: C, context: WebContext?) {
        // 検証器を作成
        val verifier = JWT
                .require(Algorithm.RSA256(authKey.get(), null))
                .withIssuer("habanero")
                .build()!!

        // 検証
        kotlin.runCatching {
            verifier.verify(credentials.token)
        }.onSuccess {
            credentials.userProfile = CommonProfile().apply { id = it.subject }
        }.onFailure {
            logger.debug(it)
        }
    }
}
