package com.sinnerschrader.s2b.accounttool.logic.component.authorization

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.*


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest
@ActiveProfiles("test")
class AuthorizationTests {

    @Autowired
    private lateinit var authorizationService: AuthorizationService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    private var admin: LdapUserDetails? = null

    private var userAdmin: LdapUserDetails? = null

    private var user: LdapUserDetails? = null

    @Before
    fun initialize() {
        val adminAuthorities = ArrayList<GrantedAuthority>()
        adminAuthorities.add(SimpleGrantedAuthority(ldapConfiguration.permissions.ldapAdminGroup))

        admin = LdapUserDetails("uid=tesadm,ou=users,ou=e1c1,dc=example,dc=org",
                "tesadm", "Test Admin", "testuser", "e1c1", adminAuthorities, false, true)

        val userAdminAuthorities = ArrayList<GrantedAuthority>()
        for (group in ldapConfiguration.permissions.userAdminGroups) {
            userAdminAuthorities.add(SimpleGrantedAuthority(group))
        }

        userAdmin = LdapUserDetails("uid=tesuse,ou=users,ou=e1c1,dc=example,dc=org",
                "tesuse", "Tes Useradmin", "testuser", "e1c1", userAdminAuthorities, false, true)

        val userAuthorities = ArrayList<GrantedAuthority>()
        for (group in Arrays
                .asList("admin-tes", "team-tes", "team-set", "devs-set", "team-err", "company-users", "company-vpn")) {
            userAuthorities.add(SimpleGrantedAuthority(group))
        }
        user = LdapUserDetails("uid=tester,ou=users,ou=e1c1,dc=example,dc=org",
                "tester", "Tes Ter", "testuser", "e1c1", userAuthorities, false, true)

    }

    @Test
    fun testPermissions() {
        authorizationService.ensureUserAdministration(admin!!)
        authorizationService.ensureGroupAdministration(admin!!, "company-users")

        Assert.assertTrue(authorizationService.isAdmin(admin!!))
        Assert.assertFalse(authorizationService.isAdmin(userAdmin!!))
        Assert.assertFalse(authorizationService.isAdmin(user!!))

        Assert.assertFalse(authorizationService.isUserAdministration(admin!!))
        Assert.assertTrue(authorizationService.isUserAdministration(userAdmin!!))
        Assert.assertFalse(authorizationService.isUserAdministration(user!!))

        Assert.assertTrue(authorizationService.isGroupAdmin(user!!, "admin-tes"))
        Assert.assertTrue(authorizationService.isGroupAdmin(user!!, "team-tes"))
        Assert.assertTrue(authorizationService.isGroupAdmin(user!!, "devs-set"))
        Assert.assertFalse(authorizationService.isGroupAdmin(user!!, "team-set"))
        Assert.assertFalse(authorizationService.isGroupAdmin(user!!, "team-err"))
    }

}
