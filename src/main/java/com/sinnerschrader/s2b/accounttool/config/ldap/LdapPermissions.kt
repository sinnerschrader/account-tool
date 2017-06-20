package com.sinnerschrader.s2b.accounttool.config.ldap

open class LdapPermissions {
    var ldapAdminGroup: String? = null
    var userAdminGroups: List<String>? = null
    var defaultGroups: List<String>? = null
}