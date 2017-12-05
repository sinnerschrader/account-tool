package com.sinnerschrader.s2b.accounttool.logic.entity

/**
 * Short user representation for better performance (cached)
 */
data class UserInfo(val uid: String,
                    val givenName: String,
                    val sn: String,
                    val o: String) : Comparable<UserInfo> {
    private fun fullName() = "$sn, $givenName"
    override fun compareTo(other: UserInfo) = fullName().compareTo(other.fullName())
}
