package com.sinnerschrader.s2b.accounttool.config;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapAuthenticationDetails;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetailsAuthenticationProvider;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.CachedLdapService;
import com.sinnerschrader.s2b.accounttool.logic.entity.UserInfo;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;


@Configuration
@EnableWebSecurity
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private LdapUserDetailsAuthenticationProvider userDetailsAuthenticationProvider;

    @Autowired
    private CachedLdapService cachedLdapService;

    @Autowired
    private LdapConfiguration ldapConfiguration;

    @Autowired
    private LdapManagementConfiguration ldapManagementConfiguration;

    @Value("${spring.security.contentSecurityPolicy}")
    private String contentSecurityPolicy;

    @Value("${server.session.cookie.secure}")
    private boolean secureCookie = false;

    @Value("${server.session.cookie.http-only}")
    private boolean httpOnlyCookie = true;

    @Override
    public void configure(WebSecurity web) throws Exception {
        log.debug("Setting up access for static resources and CSP Report");
        web.ignoring().antMatchers("/csp-report", "/extensions/**", "/static/**", "/management/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.debug("Setting up authorization");
        http.formLogin()
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .permitAll()
            .authenticationDetailsSource(authenticationDetailsSource())
            .and()
            .logout()
            .logoutUrl("/logout")
            .permitAll()
            .and()
            .authorizeRequests()
            .anyRequest()
            .authenticated()
            .and()
            .sessionManagement()
            .sessionFixation().newSession()
            .and()
            .csrf()
            .and()
            .headers()
            .contentSecurityPolicy(contentSecurityPolicy)
        ;
    }

    private WebAuthenticationDetailsSource authenticationDetailsSource() {
        return new WebAuthenticationDetailsSource() {
            @Override
            public WebAuthenticationDetails buildDetails(HttpServletRequest context) {

                try (
                    LDAPConnection connection = ldapConfiguration.createConnection()){
                    connection.bind(ldapManagementConfiguration.getUser().getBindDN(),
                            ldapManagementConfiguration.getUser().getPassword());
                    UserInfo n = cachedLdapService.getGroupMember(connection, context.getParameter("uid"));
                    return n == null ? null : new LdapAuthenticationDetails(n.getDn(), context);
                } catch (LDAPException | GeneralSecurityException e) {
                    return null;
                }
            }
        };
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(userDetailsAuthenticationProvider);
    }
}
