package com.sinnerschrader.s2b.accounttool.logic.component.encryption

import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals


@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
class PasswordCrypterTests {

    @Test
    fun testPasswordEncryption() {
        val salt = RandomStringUtils.randomAlphanumeric(16)
        val password = RandomStringUtils.randomAlphanumeric(32)

        assertEquals(Encrypt.salt(password, salt), Encrypt.salt(password, salt))
        assertNotEquals(Encrypt.salt(password), Encrypt.salt(password))
    }

}
