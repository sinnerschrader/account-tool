package com.sinnerschrader.s2b.accounttool.logic.component

import org.junit.Assert.assertEquals
import org.junit.Test

class TestPwnedPasswordService {

    @Test
    fun testPwned() =
            assertEquals("'1234' should be pwned",
                    true, PwnedPasswordService.isPwned("1234"))

    @Test
    fun testNotPwned() =
            assertEquals("'notPwnedPassword' should not be pwned",
                    false, PwnedPasswordService.isPwned("notPwnedPassword"))
}