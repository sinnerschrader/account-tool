package com.sinnerschrader.s2b.accounttool.support.pebble

import com.mitchellbosecke.pebble.extension.AbstractExtension
import com.mitchellbosecke.pebble.extension.Function
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils.currentUserDetails
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils


/**
 * Custom Extension for Pebble support which are required for the Tool
 */
@Service
class AccountToolExtension : AbstractExtension() {

    @Autowired
    private lateinit var authorizationService: AuthorizationService

    @Value("\${gravatar.domain}")
    private lateinit var gravatarDomain: String

    @Value("\${gravatar.path}")
    private lateinit var gravatarPath: String

    override fun getFunctions() = mapOf(
        "isLoggedIn" to object : Function {
            override fun getArgumentNames() = emptyList<String>()
            override fun execute(args: Map<String, Any>) = currentUserDetails != null
        },
        "isGroupAdmin" to object : Function {
            override fun getArgumentNames() = listOf("groupCn")
            override fun execute(args: Map<String, Any>) =
                    authorizationService.isGroupAdmin(currentUserDetails!!, args["groupCn"] as String)
                            || authorizationService.isAdmin(currentUserDetails!!)
        },
        "isAdmin" to object : Function {
            override fun getArgumentNames() = emptyList<String>()
            override fun execute(args: Map<String, Any>) = authorizationService.isAdmin(currentUserDetails!!)
        },
        "isUserAdmin" to object : Function {
            override fun getArgumentNames() = emptyList<String>()
            override fun execute(args: Map<String, Any>) =
                    authorizationService.isUserAdministration(currentUserDetails!!)
                            || authorizationService.isAdmin(currentUserDetails!!)
        },
        "isMemberOf" to object : Function {
            override fun getArgumentNames() = listOf("groupCn")
            override fun execute(args: Map<String, Any>): Any {
                val userDetails = currentUserDetails
                val groupArgument = args["groupCn"]
                var group: Group? = null
                val groupCn: String
                if (groupArgument is Group) {
                    group = args["groupCn"] as Group
                    groupCn = group.cn
                } else {
                    groupCn = args["groupCn"] as String
                }

                val authorities = userDetails!!.authorities
                for (ga in authorities) {
                    if (StringUtils.equals(ga.authority, groupCn)) {
                        return true
                    }
                }
                return group != null && group.hasMember(userDetails.uid, userDetails.dn)
            }
        },
        "isSelected" to object : Function {
            override fun getArgumentNames() = listOf("current", "match")
            override fun execute(args: Map<String, Any>) = if (args["current"] == args["match"]) "selected" else ""
        },
        "gravatarUrl" to object : Function {
            override fun getArgumentNames() = listOf("mail", "size")
            override fun execute(args: Map<String, Any>) =
                   with(DigestUtils.md5DigestAsHex((args["mail"] as String).toLowerCase().trim().toByteArray())) {
                       "$gravatarDomain$gravatarPath/${this}?s=${args["size"]}"
                   }
        }
    )
}
