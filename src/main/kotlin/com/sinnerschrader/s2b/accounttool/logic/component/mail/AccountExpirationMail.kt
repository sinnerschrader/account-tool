package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.logic.entity.User

class AccountExpirationMail(user: User) : Mail {
    override val subject = "[Account Tool]: Your account is expiring"
    override val body = """
        Hello ${user.givenName} ${user.sn},

        Your account is expiring on ${user.employeeExitDate}.


        Best regards,
        Your AccountTool
        """.trimIndent()
}