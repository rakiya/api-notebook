package habanero.api.client.auth

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.google.gson.Gson
import habanero.apis.clients.auth.AuthenticationKey
import habanero.app.Application
import habanero.common.RSAKeys
import io.jooby.Environment
import io.jooby.JoobyTest
import org.junit.jupiter.api.*
import org.koin.core.KoinComponent
import org.koin.core.context.stopKoin
import org.koin.core.inject
import java.lang.Thread.sleep
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

@JoobyTest(Application::class)
class PublicKeyClientTest : KoinComponent {

    private val env: Environment by inject()

    private val authKey: AuthenticationKey by inject()

    companion object {
        @JvmField
        val mockServer = WireMockServer(options().port(8123))

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            val json = Gson()
                    .toJson(
                            mapOf("publicKey" to Base64.getEncoder().encodeToString(RSAKeys.publicKey.encoded))
                    )

            // モックサーバの定義
            configureMockServer(json)
        }

        @Suppress("unused")
        @JvmStatic
        @AfterAll
        fun afterAll() {
            mockServer.stop()
            stopKoin()
        }

        private fun configureMockServer(json: String) {
            if (mockServer.isRunning) mockServer.stop()

            mockServer.stubFor(
                    get(urlEqualTo("/publicKey"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(json)
                            )
            )

            mockServer.start()
        }
    }

    @Test
    @DisplayName("初回の公開鍵ができること")
    fun `Should return the public key when the first time`() {
        val actual = authKey.get()
        validate(RSAKeys.publicKey, actual)
    }

    @Test
    @DisplayName("指定時間経過後に公開鍵の取得ができること")
    fun `Should return the public key after the certain time`() {
        // 1回目
        authKey.get()

        // 間隔を開ける
        sleep(env.config.getLong("api.auth.interval") * 1000)

        // 新しい公開鍵を作成
        val publicKey = KeyPairGenerator.getInstance("RSA")
                .apply { initialize(2048) }
                .run { generateKeyPair()!! }
                .let {
                    val factory = KeyFactory.getInstance("RSA")!!
                    val spec = factory.getKeySpec(it.public, X509EncodedKeySpec::class.java)!!
                    factory.generatePublic(spec)!!
                }

        //新しい鍵を設定
        configureMockServer(
                Gson().toJson(
                        mapOf(
                                "publicKey" to Base64.getEncoder().encodeToString(publicKey.encoded)
                        )
                )
        )

        // 2回目
        val actual = authKey.get()
        validate(publicKey, actual)

        // 後処理
        configureMockServer(
                Gson().toJson(
                        mapOf(
                                "publicKey" to Base64.getEncoder().encodeToString(RSAKeys.publicKey.encoded)
                        )
                )
        )
    }

    private fun validate(expected: PublicKey, actual: PublicKey?) {
        Assertions.assertNotNull(actual)
        Assertions.assertEquals(expected, actual)
    }
}