package com.sinnerschrader.s2b.accounttool.logic.component.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import java.math.BigInteger
import java.security.MessageDigest

/**
 * @see https://haveibeenpwned.com/API/v2#SearchingPwnedPasswordsByRange
 */
object PwnedPasswordService {
    private val LOG = LoggerFactory.getLogger(PwnedPasswordService::class.java)

    private class Hash(password: String) {
        private val sha1 = sha1(password).toUpperCase()
        val prefix = sha1.substring(0, 5)
        val suffix = sha1.substring(5)

        private fun sha1(input: String) =
                try {
                    with(MessageDigest.getInstance("SHA-1")) {
                        reset()
                        update(input.toByteArray(Charsets.UTF_8))
                        String.format("%040x", BigInteger(1, digest()))
                    }
                } catch (e: Exception) {
                    LOG.error("Failed to create SHA-1 hash")
                    ""
                }
    }

    private fun retrieveList(prefix: String): List<String> {
        val headers = HttpHeaders().apply { add("user-agent", "github.com/sinnerschrader/account-tool") }

        try {
            val template = RestTemplate().exchange(
                    "https://api.pwnedpasswords.com/range/$prefix",
                    HttpMethod.GET,
                    HttpEntity<String>("parameters", headers),
                    String::class.java)

            return when (template.statusCode) {
                HttpStatus.OK -> template.body.lines()
                else -> {
                    LOG.warn("Failed to retrieve list of pwnedpasswords: $this")
                    emptyList()
                }
            }
        } catch(e: Exception) {
            return emptyList()
        }
    }

    fun isPwned(password: String) = with(Hash(password)) { retrieveList(prefix).any { it.startsWith("$suffix:") } }
}