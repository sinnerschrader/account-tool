package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.logic.component.mail.AccountChangeMail.Action.PASSWORD_CHANGED
import com.sinnerschrader.s2b.accounttool.logic.entity.User

class AccountChangeMail(user: User, action: Action) : Mail {
    enum class Action { PASSWORD_CHANGED, SSH_KEY_CHANGED }

    override val subject = "[Account Tool]: Changes were made in your account"

    override val body = """
    Hello ${user.givenName} ${user.sn},

    ${when (action) {
        PASSWORD_CHANGED -> "Your password was changed."
        else -> "Your SSH Public Key was changed."
    }}
    If this was you then you can safely ignore this mail.

    If this wasn't you please call the number on your Emergency Card


    Best regards,
    Your AccountTool
    """.trimIndent()
}