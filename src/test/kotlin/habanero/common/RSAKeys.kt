package habanero.common

import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

object RSAKeys {
    val privateKey = readPrivateKey()
    val publicKey = readPublicKey()

    /**
     * 秘密鍵を読み込む。
     *
     * @param path String 秘密鍵のパス
     * @return RSAPrivateKey 読み込んだ秘密鍵のオブジェクト
     */
    private fun readPrivateKey(): RSAPrivateKey {
        val privateKeyContent = String(File("src/test/resources/private_key.pem").readBytes())
                .replace(Regex("-+.+PRIVATE KEY-+\n"), "")
                .replace("\n", "")
        val keySpec = PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(privateKeyContent))
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
    }

    /**
     * 公開鍵を読み込む。
     *
     * @param path String 公開鍵のパス
     * @return RSAPublicKey 読み込んだ公開鍵のオブジェクト
     */
    private fun readPublicKey(): RSAPublicKey {
        val publicKeyAsText = File("src/test/resources/public_key.pem").readText()
                .replace(Regex("-+.+PUBLIC KEY-+\n"), "")
                .replace("\n", "")
        val keySpec = X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicKeyAsText))
        return KeyFactory.getInstance("RSA").generatePublic(keySpec) as RSAPublicKey
    }
}