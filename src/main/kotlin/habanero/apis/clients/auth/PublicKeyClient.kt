package habanero.apis.clients.auth

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import habanero.exceptions.HabaneroInitializationException
import io.jooby.Environment
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDateTime
import java.util.*

/**
 * ユーザの認証に必要な公開鍵を管理するクラス
 *
 * @author Ryutaro Akiya
 */
class AuthenticationKey : KoinComponent {

    /**
     * アプリケーションの環境
     */
    private val env: Environment by inject()

    /**
     * 公開鍵の取得を同時におこないわないためのミューテックス
     */
    private val mutex = Mutex()

    /**
     * 取得した公開鍵
     */
    private var publicKey: RSAPublicKey? = null

    /**
     * 鍵を取得した時間
     */
    private var updatedAt: LocalDateTime? = null

    /**
     * 認証サーバのAPIからのレスポンス
     * @property publicKey String 公開鍵
     */
    private data class Response(val publicKey: String)

    /**
     * APIからアカウント認証用公開鍵を取得して返す。
     * 鍵の取得(更新)は、鍵が未取得か、取得してから1時間以上経過している場合に行う。
     * 鍵が取得できなかった場合、以前取得した鍵を返す。
     *
     * @return RSAPublicKey 公開鍵
     */
    fun get(): RSAPublicKey = runBlocking {
        // 同時に取得しないようにロック
        mutex.lock()

        // 鍵を取得すべきか(前回の取得からの経過時間や鍵の取得状況などで判断)
        if (!shouldUpdate()) {
            // 取得すべきでないなら前回取得した鍵を返す
            mutex.unlock()
            return@runBlocking publicKey!!
        }

        // 鍵を取得
        val (_, response, result) =
                "${env.config.getString("api.auth.url")}/publicKey".httpGet().response()

        // 鍵の取得に成功した場合
        if (result is Result.Success) {
            // 取得した鍵をオブジェクトに変換
            kotlin.runCatching {
                String(response.data)
                        .let { Gson().fromJson(it, Response::class.java) }
                        .let { Base64.getMimeDecoder().decode(it.publicKey) }
                        .let { X509EncodedKeySpec(it) }
                        .let { KeyFactory.getInstance("RSA").generatePublic(it) }
                        .let { it as RSAPublicKey }
            }.onSuccess {
                publicKey = it
                updatedAt = LocalDateTime.now()
            }
        }

        mutex.unlock()

        // 初回で鍵を取得できない場合はエラーとなり、アプリケーションを終了
        if (publicKey == null) {
            throw HabaneroInitializationException("公開鍵を取得できません")
        }

        return@runBlocking publicKey!!
    }

    /**
     * 公開鍵を取得すべきかを判定する。
     *
     * @return Boolean 取得すべき場合true、しなくていい場合false。
     */
    private fun shouldUpdate(): Boolean {
        val interval = env.config.getLong("api.auth.interval")

        return updatedAt == null || updatedAt!!.plusSeconds(interval).isBefore(LocalDateTime.now())
    }
}
