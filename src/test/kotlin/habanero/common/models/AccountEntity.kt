package habanero.common.models

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import habanero.common.RSAKeys
import java.time.ZonedDateTime
import java.util.*

class AccountEntity(val id: String) {

    /**
     * ログインを行う。アクセストークンを返す。
     * @return String アクセストークン
     */
    fun login(): String {
        val issuedDate = ZonedDateTime.now()
        val expiredDate = issuedDate.plusSeconds(10)
        return JWT.create()
                .withIssuer("habanero")
                .withSubject(this.id)
                .withNotBefore(Date.from(issuedDate.toInstant()))
                .withIssuedAt(Date.from(issuedDate.toInstant()))
                .withExpiresAt(Date.from(expiredDate.toInstant()))
                .withClaim("rft", this.id)
                .sign(Algorithm.RSA256(RSAKeys.publicKey, RSAKeys.privateKey))
    }
}