package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.logic.entity.User

class GroupChangeMail(currentUser: LdapUserDetails, user: User, group: Group, action: Action) : Mail {
    enum class Action(val text: String) { ADD("added"), REMOVE("removed") }

    override val subject = "[Account Tool]: User ${user.uid} was ${action.text} group ${group.cn}"
    override val body = """
        Hello admin of ${group.cn},

        user ${user.uid} was ${action.text} by ${currentUser.uid}


        Best regards,
        Your AccountTool
        """.trimIndent()
}