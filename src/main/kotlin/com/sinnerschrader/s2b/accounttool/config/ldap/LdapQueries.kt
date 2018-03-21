package com.sinnerschrader.s2b.accounttool.config.ldap

object LdapQueries {
    val searchUser= "(&(objectClass=posixAccount)(|(uid={0})(givenName={0})(sn={0})(mail={0})(cn={0})))"
    val findUserByUid= "(&(objectClass=posixAccount)(uid={0}))"
    val findGroupByCn= "(&(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames)(objectClass=groupOfNames))(cn={0}))"
    val findGroupsByUser= "(|(&(objectClass=posixGroup)(memberUid={0}))(&(objectClass=groupOfUniqueNames)(uniqueMember={1}))(&(objectClass=groupOfNames)(member={1})))"
    val listAllGroups= "(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames)(objectClass=groupOfNames))"
    val listAllUsers= "(&(objectClass=inetOrgPerson)(objectClass=posixAccount))"
    val findUserByUidNumber= "(&(objectClass=posixAccount)(uidNumber={0}))"
    val checkUniqAttribute= "(&(objectClass=posixAccount)({0}={1}))"
}
