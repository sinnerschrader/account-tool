package com.sinnerschrader.s2b.accounttool.config.ldap

object LdapQueries {
    const val searchUser= "(&(objectClass=posixAccount)(|(uid={0})(givenName={0})(sn={0})(mail={0})(cn={0})))"
    const val findUserByUid= "(&(objectClass=posixAccount)(uid={0}))"
    const val findGroupByCn= "(&(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames)(objectClass=groupOfNames))(cn={0}))"
    const val findGroupsByUser= "(|(&(objectClass=posixGroup)(memberUid={0}))(&(objectClass=groupOfUniqueNames)(uniqueMember={1}))(&(objectClass=groupOfNames)(member={1})))"
    const val listAllGroups= "(|(objectClass=posixGroup)(objectClass=groupOfUniqueNames)(objectClass=groupOfNames))"
    const val listAllUsers= "(&(objectClass=inetOrgPerson)(objectClass=posixAccount))"
    const val findUserByUidNumber= "(&(objectClass=posixAccount)(uidNumber={0}))"
    const val checkUniqAttribute= "(&(objectClass=posixAccount)({0}={1}))"
}
