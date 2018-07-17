package com.sinnerschrader.s2b.accounttool.config.ldap

object LdapQueries {
    fun searchUser(searchTerm: String)= "(&(objectClass=posixAccount)(|(uid=$searchTerm)(givenName=$searchTerm)(sn=$searchTerm)(mail=$searchTerm)(cn=$searchTerm)))"
    fun findUserByUid(uid: String)= "(&(objectClass=posixAccount)(uid=$uid))"
    fun findUserByUidNumber(uidNumber: String)= "(&(objectClass=posixAccount)(uidNumber=$uidNumber))"
    fun findGroupByCn(cn: String)= "(&(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames)(objectClass=groupOfNames))(cn=$cn))"
    fun findGroupsByUser(uid: String, userDN: String)= "(|(&(objectClass=posixGroup)(memberUid=$uid))(&(objectClass=groupOfUniqueNames)(uniqueMember=$userDN))(&(objectClass=groupOfNames)(member=$userDN)))"
    const val listAllGroups= "(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames)(objectClass=groupOfNames))"
    const val listAllUsers= "(&(objectClass=inetOrgPerson)(objectClass=posixAccount))"
    fun checkUniqAttribute(attribute: String, value: String)= "(&(objectClass=posixAccount)($attribute=$value))"
}
