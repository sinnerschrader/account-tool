package com.sinnerschrader.s2b.accounttool.logic.entity

/**
 * Short user representation for better performance (cached)
 */
data class UserInfo(val dn: String,
                    val uid: String,
                    val givenName: String,
                    val sn: String,
                    val o: String,
                    val mail: String,
                    val szzStatus: User.State,
                    val type: String,
                    val externalAccounts: Map<String, String> = emptyMap()) : Comparable<UserInfo> {
    private fun fullName() = "$sn, $givenName"
    override fun compareTo(other: UserInfo) = fullName().compareTo(other.fullName())

    constructor(user:User) : this(
                dn = user.dn,
                uid = user.uid,
                givenName = user.givenName,
                sn = user.sn,
                o = user.o,
                mail = user.mail,
                szzStatus = user.szzStatus,
                type = user.description,
                externalAccounts = user.szzExternalAccounts)
}
