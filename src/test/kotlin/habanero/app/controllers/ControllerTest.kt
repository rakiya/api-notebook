package habanero.app.controllers

import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.google.gson.Gson
import habanero.app.responses.ErrorResponse
import habanero.app.responses.NoContentResponse
import habanero.common.RSAKeys
import habanero.common.models.AccountEntity
import habanero.extensions.gson.TestGsonBuilder
import io.jooby.Environment
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import java.util.*

@Suppress("unused")
abstract class ControllerTest : KoinTest {

    val logger: Log = LogFactory.getLog("test")

    companion object {
        const val url = "http://localhost:8911"

        private val environment: Environment by inject(Environment::class.java)

        private val authServer = WireMockServer(
                environment.config.getString("api.auth.url").split(":").last().toInt()
        ).also { server ->
            if (server.isRunning) server.stop()

            val json = RSAKeys.publicKey.encoded
                    .let { Base64.getEncoder().encodeToString(it) }
                    .let { Gson().toJson(mapOf("publicKey" to it)) }

            // レスポンスをスタブに登録
            WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)
                    .let {
                        // レスポンスをパスに関連付け
                        WireMock
                                .get(WireMock.urlEqualTo("/publicKey"))
                                .willReturn(it)
                    }
                    .let {
                        // パスを登録
                        server.stubFor(it)
                    }

            server.start()
        }

        val db: DSLContext = DSL.using(
                environment.config.getString("db.url"),
                environment.config.getString("db.username"),
                environment.config.getString("db.password")
        )

        @AfterAll
        @JvmStatic
        fun afterAll() {
            authServer.stop()
            stopKoin()
        }
    }

    data class ResponseEntity<T>(val body: T, val statusCode: Int)

    inline fun <reified T> get(
            path: String,
            account: AccountEntity? = null,
            block: (ResponseEntity<T>) -> Unit = {}
    ) {
        val request = (url + path).httpGet().apply {
            header("Content-Type", "application/json")
            if (account != null)
                header("Authorization", "Bearer ${account.login()}")
        }

        // NOTE サーバが起動する前にテストが開始した
        while (true) {
            val (_, response, _) = request.response()

            logger.debug(String(response.data))

            val body = String(response.data)
                    .let { TestGsonBuilder.build().fromJson(it, T::class.java) }
            val status = response.statusCode

            if (status == -1) {
                Thread.sleep(1000)
                continue
            } else {
                block(ResponseEntity(body, status))
                break
            }

        }
    }

    inline fun <reified RES, REQ> post(
            path: String,
            body: REQ? = null,
            account: AccountEntity? = null,
            block: (ResponseEntity<RES>) -> Unit = {}
    ) {
        val request = (url + path).httpPost().apply {
            header("Content-Type", "application/json")
            if (account != null)
                header("Authorization", "Bearer ${account.login()}")
            if (body != null)
                body(TestGsonBuilder.build().toJson(body))
        }

        // NOTE サーバが起動する前にテストが開始した
        while (true) {
            val (_, response, _) = request.response()

            logger.debug(response.statusCode)
            logger.debug(String(response.data))

            val responseBody = String(response.data)
                    .let { TestGsonBuilder.build().fromJson(it, RES::class.java) }
            val status = response.statusCode

            if (status == -1) {
                Thread.sleep(1000)
                continue
            } else {
                block(ResponseEntity(responseBody, status))
                break
            }

        }
    }

    inline fun <reified RES, REQ> put(
            path: String,
            body: REQ? = null,
            account: AccountEntity? = null,
            block: (ResponseEntity<RES>) -> Unit = {}
    ) {
        val request = (url + path).httpPut().apply {
            header("Content-Type", "application/json")
            if (account != null)
                header("Authorization", "Bearer ${account.login()}")
            if (body != null)
                body(TestGsonBuilder.build().toJson(body))
        }

        logger.debug(request)

        // NOTE サーバが起動する前にテストが開始した
        while (true) {
            val (_, response, _) = request.response()

            logger.debug(response.statusCode)
            logger.debug(String(response.data))

            val responseBody = String(response.data)
                    .let { TestGsonBuilder.build().fromJson(it, RES::class.java) }
            val status = response.statusCode

            if (status == -1) {
                Thread.sleep(1000)
                continue
            } else {
                block(ResponseEntity(responseBody, status))
                break
            }

        }
    }

    inline fun <reified RES> delete(
            path: String,
            account: AccountEntity? = null,
            block: (ResponseEntity<RES>) -> Unit = {}
    ) {
        val request = (url + path).httpDelete().apply {
            header("Content-Type", "application/json")
            if (account != null)
                header("Authorization", "Bearer ${account.login()}")
        }

        // NOTE サーバが起動する前にテストが開始した
        while (true) {
            val (_, response, _) = request.response()

            logger.debug(String(response.data))

            val responseBody = String(response.data)
                    .let { TestGsonBuilder.build().fromJson(it, RES::class.java) }
            val status = response.statusCode

            if (status == -1) {
                Thread.sleep(1000)
                continue
            } else {
                block(ResponseEntity(responseBody, status))
                break
            }

        }
    }

    protected fun validateFailure(expected: ErrorResponse, actual: ResponseEntity<ErrorResponse>) {
        Assert.assertEquals(expected.statusCode!!.value(), actual.statusCode)

        Assert.assertEquals(expected.errors.size, actual.body.errors.size)

        actual.body.errors.forEach { actualError ->
            val expectedError = expected.errors.find { it.field == actualError.field }
            Assert.assertNotNull(expectedError)

            actualError.descriptions.forEach { actualMessage ->
                val expectedMessage = expectedError!!.descriptions.find { it == actualMessage }
                Assert.assertNotNull(
                        "\"$expectedMessage\"が存在しません",
                        actualMessage
                )
            }
        }
    }

    protected fun validateNoContent(expected: NoContentResponse, actual: ResponseEntity<NoContentResponse>) {
        Assert.assertEquals(expected.statusCode!!.value(), actual.statusCode)
    }

    protected fun <T : TableImpl<*>> validateRecordCount(diff: Int, table: T, mainProcess: () -> Unit) {
        val countBefore = db.selectCount().from(table).fetchOne().into(Int::class.java)

        mainProcess()

        val countAfter = db.selectCount().from(table).fetchOne().into(Int::class.java)

        Assert.assertEquals("${countBefore}から${countAfter}に変化しました。", diff, countAfter - countBefore)
    }

    protected fun validateRecordCount(vararg diffs: Pair<TableImpl<*>, Int>, mainProcess: () -> Unit) {
        val countBefore = diffs.map { (table, _) ->
            val count = db.selectCount().from(table).fetchOne().into(Int::class.java)
            table to count
        }.toMap()

        mainProcess()

        val countAfter = diffs.map { (table, _) ->
            val count = db.selectCount().from(table).fetchOne().into(Int::class.java)
            table to count
        }.toMap()

        diffs.forEach { (table, diff) ->
            Assert.assertEquals(
                    "${table}のレコード数が${countBefore[table]}から${countAfter[table]}に変化しました。",
                    diff,
                    countAfter.getValue(table) - countBefore.getValue(table)
            )
        }
    }
}
