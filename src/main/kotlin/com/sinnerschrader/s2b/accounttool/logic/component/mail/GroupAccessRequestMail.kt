package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.logic.entity.Group

class GroupAccessRequestMail(user: LdapUserDetails, group: Group, adminGroup: Group) : Mail {
    override val subject = "[Account Tool]: Requested access for group ${group.cn} from ${user.uid}"
    override val body = """
         Hello Admins,

         the user ${user.displayName} (${user.uid}) requested the access for the LDAP Group ${group.cn}.
         Please add him/her to the Group.

         You receive this mails, because you are member of ${adminGroup.cn}


         Best regards,
         Your AccountTool
         """.trimIndent()
}