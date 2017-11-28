package com.sinnerschrader.s2b.accounttool.logic.component.encryption;

import org.apache.commons.codec.digest.Crypt
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.security.crypto.codec.Hex
import sun.security.provider.MD4
import java.io.UnsupportedEncodingException

object Encrypt {
    @JvmStatic fun salt(password: String) = salt(password, RandomStringUtils.randomAlphanumeric(16))
    @JvmStatic fun salt(password: String, salt: String): String {
        if (password.isEmpty() or salt.isEmpty()) {
            throw IllegalArgumentException("Password and salt can't be null or empty")
        }
        return "{CRYPT}" + Crypt.crypt(password, "$6$" + salt)
    }

    @JvmStatic fun samba(password: String): String {
        if (password.isEmpty()) {
            throw IllegalArgumentException("Password can't be null or empty")
        }
        try {
            return String(Hex.encode(MD4.getInstance().digest(password.toByteArray(Charsets.UTF_16LE)))).toUpperCase()
        } catch (uee: UnsupportedEncodingException) {
            throw RuntimeException("Could not generate samba password hash", uee)
        }
    }
}
