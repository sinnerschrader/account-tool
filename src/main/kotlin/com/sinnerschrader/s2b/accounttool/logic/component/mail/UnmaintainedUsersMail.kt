package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.logic.entity.User

class UnmaintainedUsersMail(leavingUsers: List<User>, unmaintainedMailUsers: List<User>, unmaintainedUsers: List<User>, publicDomain: String) : Mail {
    override val subject = "[Account Tool]: Request for Account maintenance"
    override val body = """
        Hello Team Usermanagement,

        you receive this mail, because you are on the receipient list or group for accounts notifications.

        Overview:
        * ${leavingUsers.size} will leave in the next Weeks
        * ${unmaintainedMailUsers.size} inactive Accounts have active Mail accounts
        * ${unmaintainedUsers.size} Accounts are active but have already left the company

        You will find a detailed view here: https://${publicDomain}/user/maintenance

        Best regards,
        Your AccountTool
        """.trimIndent()
}