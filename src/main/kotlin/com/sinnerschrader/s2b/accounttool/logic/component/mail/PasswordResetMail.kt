package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.logic.entity.User

class PasswordResetMail(currentUser: User, user: User, password: String, publicDomain: String) : Mail {
    override val subject = "[Account Tool]: Your password has been reset"
    override val body = """
        Hello ${user.displayName},

        your password has been reset by user ${currentUser.uid}.


        The new password is ${password}


        Please change your password immediately. You can change it here: https://${publicDomain}


        Best regards,
        Your AccountTool
        """.trimIndent()
}