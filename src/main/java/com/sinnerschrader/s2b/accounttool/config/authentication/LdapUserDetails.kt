package com.sinnerschrader.s2b.accounttool.config.authentication

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable


class LdapUserDetails(val dn: String,
                      val uid: String,
                      val displayName: String,
                      private var password: String?,
                      val company: String,
                      grantedAuthorities: List<GrantedAuthority>?,
                      private val expired: Boolean,
                      private val enabled: Boolean) : UserDetails, Serializable {

    private val grantedAuthorities = grantedAuthorities?.toList() ?: emptyList()

    override fun getPassword() = password

    fun setPassword(password: String) {
        this.password = password
    }

    override fun getUsername() = uid

    override fun getAuthorities() = grantedAuthorities

    override fun isAccountNonExpired() = !expired

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = enabled
}
