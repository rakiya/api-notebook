package habanero.modules.security.credentials.authenticators

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import habanero.apis.clients.auth.AuthenticationKey
import habanero.common.RSAKeys
import habanero.common.models.AccountEntity
import org.junit.jupiter.api.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.pac4j.core.credentials.TokenCredentials

class JwtAuthenticatorTest {

    companion object {
        private val authKeyMock = mock<AuthenticationKey> {
            on { get() } doReturn RSAKeys.publicKey
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            startKoin { modules(module { single { authKeyMock } }) }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            stopKoin()
        }
    }

    private val jwtAuthenticator = JwtAuthenticator<TokenCredentials>()

    @Test
    @DisplayName("認証ができること")
    fun `Should authenticate a token`() {
        val account = AccountEntity("1")
        val accessToken = account.login()
        val credentials = TokenCredentials(accessToken)
        jwtAuthenticator.validate(credentials, mock())

        Assertions.assertNotNull(credentials.userProfile)
        Assertions.assertEquals(account.id, credentials.userProfile.id)
    }

    @Test
    @DisplayName("不正なトークンを認証しないこと")
    fun `Should not authenticate an invalid token`() {
        val accessToken = AccountEntity("1").login().substring(1)
        val credentials = TokenCredentials(accessToken)
        jwtAuthenticator.validate(credentials, mock())

        Assertions.assertNull(credentials.userProfile)
    }
}