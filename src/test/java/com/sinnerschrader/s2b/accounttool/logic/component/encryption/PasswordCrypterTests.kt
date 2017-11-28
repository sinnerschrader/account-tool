package com.sinnerschrader.s2b.accounttool.logic.component.encryption

import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
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
    fun testRandom() {
        val salt = RandomStringUtils.randomAlphanumeric(16)
        val password = RandomStringUtils.randomAlphanumeric(32)

        assertEquals(Encrypt.salt(password, salt), Encrypt.salt(password, salt))
        assertNotEquals(Encrypt.salt(password), Encrypt.salt(password))
    }

    @Test
    fun testSpecific() {
        val salt = Encrypt.salt("password", "salt")
        assertEquals("{CRYPT}$6\$salt\$IxDD3jeSOb5eB1CX5LBsqZFVkJdido3OUILO5Ifz5iwMuTS4XMS130MTSuDDl3aCI6WouIL9AjRbLCelDCy.g.", salt)
        val samba = Encrypt.samba("password")
        assertEquals("8846F7EAEE8FB117AD06BDD830B7586C", samba)
    }

}
