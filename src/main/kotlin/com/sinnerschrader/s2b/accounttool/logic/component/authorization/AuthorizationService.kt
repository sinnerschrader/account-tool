package com.sinnerschrader.s2b.accounttool.logic.component.authorization

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service


@Service
class AuthorizationService {

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    private fun isMemberOf(authorities: Collection<GrantedAuthority>, group: String?): Boolean {
        for (ga in authorities)
            if (StringUtils.equals(ga.authority, group))
                return true
        return false
    }

    fun isAdmin(user: LdapUserDetails) = isMemberOf(user.authorities, ldapConfiguration.permissions.ldapAdminGroup)

    fun isUserAdministration(user: LdapUserDetails): Boolean {
        for (userAdminGroup in ldapConfiguration.permissions.userAdminGroups)
            if (isMemberOf(user.authorities, userAdminGroup))
                return true
        return false
    }

    fun isGroupAdmin(user: LdapUserDetails, groupCn: String): Boolean {
        val prefixSuffix = "-" //yepp, a suffix on a prefix.
        val adminPrefix = ldapConfiguration.groupPrefixes.admin + prefixSuffix
        val technicalPrefix = ldapConfiguration.groupPrefixes.technical + prefixSuffix
        val teamPrefix = ldapConfiguration.groupPrefixes.team + prefixSuffix
        if (StringUtils.startsWith(groupCn, adminPrefix) || StringUtils.startsWith(groupCn, technicalPrefix)) {
            return isMemberOf(user.authorities, groupCn)
        }
        return if (StringUtils.startsWith(groupCn, teamPrefix)) {
            isMemberOf(user.authorities, StringUtils.replace(groupCn, teamPrefix, adminPrefix))
        } else false
    }

    fun ensureUserAdministration() =  with(RequestUtils.currentUserDetails!!){ // default parameter failed to work with @PreAuth
        isAdmin(this) || isUserAdministration(this)
    }
    @Throws(AccessDeniedException::class)
    fun ensureUserAdministration(user: LdapUserDetails) {
        if (isAdmin(user) || isUserAdministration(user)) {
            return
        }
        throw AccessDeniedException("User is not member of required groups [user=$user]")
    }

    @Throws(AccessDeniedException::class)
    fun ensureGroupAdministration(user: LdapUserDetails, group: String) {
        if (isAdmin(user) || isGroupAdmin(user, group)) {
            return
        }
        throw AccessDeniedException("User is not member of required group [user=$user]")
    }

}
