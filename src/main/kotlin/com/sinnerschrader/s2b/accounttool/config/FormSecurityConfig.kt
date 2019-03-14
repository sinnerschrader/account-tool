package com.sinnerschrader.s2b.accounttool.config

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapAuthenticationDetails
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetailsAuthenticationProvider
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.PwnedAuthenticationSuccessHandler
import com.unboundid.ldap.sdk.LDAPException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import java.security.GeneralSecurityException
import javax.servlet.http.HttpServletRequest


@EnableWebSecurity
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
@EnableGlobalMethodSecurity(securedEnabled = true)
class FormSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    private lateinit var userDetailsAuthenticationProvider: LdapUserDetailsAuthenticationProvider

    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var ldapManagementConfiguration: LdapManagementConfiguration

    @Value("\${spring.security.contentSecurityPolicy}")
    private lateinit var contentSecurityPolicy: String

    @Throws(Exception::class)
    override fun configure(web: WebSecurity) {
        LOG.debug("Setting up access for static resources and CSP Report")
        web.ignoring().antMatchers(
                "/csp-report",
                "/extensions/**",
                "/static/**",
                "/management/**",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/v2/api-docs",
                "/webjars/**"
        )
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        LOG.debug("Setting up authorization")
        http.formLogin()
                .successHandler(PwnedAuthenticationSuccessHandler)
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .permitAll()
                .authenticationDetailsSource(authenticationDetailsSource())
        http.logout()
                .logoutUrl("/logout")
                .permitAll()
        http.authorizeRequests()
                .anyRequest()
                .authenticated()
        http.sessionManagement()
                .sessionFixation()
                .newSession()
        http.csrf()
        http.headers()
                .contentSecurityPolicy(contentSecurityPolicy)

    }

    private fun authenticationDetailsSource(): WebAuthenticationDetailsSource {
        return object : WebAuthenticationDetailsSource() {
            override fun buildDetails(context: HttpServletRequest): WebAuthenticationDetails? {
                try {
                    ldapConfiguration.createConnection().use { connection ->
                        with(ldapManagementConfiguration.user) {
                            connection.bind(bindDN, password)
                        }
                        return LdapAuthenticationDetails(context) { ldapService.getGroupMember(connection, it).dn } // TODO verify exists
                    }
                } catch (e: LDAPException) {
                    // TODO log exception
                    return null
                } catch (e: GeneralSecurityException) {
                    // TODO log exception
                    return null
                }
            }
        }
    }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) = auth.authenticationProvider(userDetailsAuthenticationProvider)

    companion object {
        private val LOG = LoggerFactory.getLogger(FormSecurityConfig::class.java)
    }
}
