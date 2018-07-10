package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.unboundid.ldap.sdk.LDAPConnection
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.util.*
import kotlin.streams.toList


@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
class LdapServiceTests {

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var ldapService: LdapService

    @Value("\${test.ldap.user}")
    private val ldapUser = ""

    @Value("\${test.ldap.company}")
    private val ldapUserCompany = ""

    @Value("\${test.ldap.password}")
    private val ldapUserPassword = ""

    private var currentUser: LdapUserDetails? = null

    @Throws(Exception::class)
    private fun create(bind: Boolean): LDAPConnection {
        val connection = ldapConfiguration.createConnection()
        if (bind) {
            val bindDN = ldapConfiguration.getUserBind(ldapUser, ldapUserCompany)
            connection.bind(bindDN, ldapUserPassword)
        }
        return connection
    }

    @Before
    @Throws(Exception::class)
    fun login() {
        val connection = create(false)

        val user = ldapService.getUserByUid(connection, ldapUser)
        Assert.assertNotNull(user)

        val groups = ldapService.getGroupsByUser(connection, user!!.uid, user.dn)
        Assert.assertNotNull(groups)
        assertFalse(groups.isEmpty())

        val grantedAuthorities = groups.stream()
                .map { group -> SimpleGrantedAuthority(group.cn) }
                .toList()

        this.currentUser = LdapUserDetails(user.dn, user.uid, user.displayName!!,
                ldapUserPassword, ldapUserCompany, grantedAuthorities, false, true)

        connection.close()
    }

    @Test
    @Throws(Exception::class)
    fun testReadOperations() {
        val connection = create(true)

        val uid = "dontexists"
        val userDN = "uid=dontexists,ou=users,ou=comp,dc=example,dc=org"

        Assert.assertNull(ldapService.getUserByUid(connection, uid))
        val userGroups = ldapService.getGroupsByUser(connection, uid, userDN)
        Assert.assertNotNull(userGroups)
        assertTrue(userGroups.isEmpty())

        val allGroups = ldapService.getGroups(connection)
        val group = ldapService.getGroupByCN(connection, "company-vpn")
        val dontExist = ldapService.getGroupByCN(connection, "thisgroupwillneverexist-hopefully")

        Assert.assertNotNull(allGroups)
        assertFalse(allGroups.isEmpty())
        Assert.assertNotNull(group)
        assertTrue(allGroups.contains(group))
        Assert.assertNull(dontExist)

        connection.close()
    }

    @Test
    @Throws(Exception::class)
    fun testGroupTypes() {
        val connection = create(true)

        val userToAdd = ldapService.getUserByUid(connection, "musmax")
        var posixGroup = ldapService.getGroupByCN(connection, "team-cus")
        var groupOfNames = ldapService.getGroupByCN(connection, "groupOfNames")
        var groupOfUNames = ldapService.getGroupByCN(connection, "groupOfUniqueNames")

        assertTrue(posixGroup!!.groupType === Group.GroupType.Posix)
        assertTrue(groupOfNames!!.groupType === Group.GroupType.GroupOfNames)
        assertTrue(groupOfUNames!!.groupType === Group.GroupType.GroupOfUniqueNames)

        assertFalse(posixGroup!!.hasMember(userToAdd!!.uid, userToAdd.dn))
        assertFalse(groupOfNames!!.hasMember(userToAdd.uid, userToAdd.dn))
        assertFalse(groupOfUNames!!.hasMember(userToAdd.uid, userToAdd.dn))

        posixGroup = ldapService.addUserToGroup(connection, userToAdd, posixGroup)
        groupOfNames = ldapService.addUserToGroup(connection, userToAdd, groupOfNames)
        groupOfUNames = ldapService.addUserToGroup(connection, userToAdd, groupOfUNames)

        assertTrue(posixGroup!!.hasMember(userToAdd.uid, userToAdd.dn))
        assertTrue(groupOfNames!!.hasMember(userToAdd.uid, userToAdd.dn))
        assertTrue(groupOfUNames!!.hasMember(userToAdd.uid, userToAdd.dn))

        posixGroup = ldapService.removeUserFromGroup(connection, userToAdd, posixGroup)
        groupOfNames = ldapService.removeUserFromGroup(connection, userToAdd, groupOfNames)
        groupOfUNames = ldapService.removeUserFromGroup(connection, userToAdd, groupOfUNames)

        assertFalse(posixGroup!!.hasMember(userToAdd.uid, userToAdd.dn))
        assertFalse(groupOfNames!!.hasMember(userToAdd.uid, userToAdd.dn))
        assertFalse(groupOfUNames!!.hasMember(userToAdd.uid, userToAdd.dn))

        connection.close()
    }

    private fun createUser(firstName: String = rnd(), lastName: String = rnd(), uid: String = "", email: String = "", employeeNumber: String = ""): User {
        return User("",
                uid = uid,
                givenName = firstName,
                sn = lastName,
                birthDate = LocalDate.of(1972, 7, 1),
                mail = email,
                szzStatus =  User.State.active,
                szzMailStatus = User.State.active,
                employeeEntryDate =  LocalDate.of(1990, 1, 1),
                employeeExitDate = LocalDate.of(2100, 12, 31),
                ou =  "Team Instinct",
                description =  "Imaginary Employee",
                employeeNumber =  employeeNumber,
                l = "Hamburg",
                o = "Example - Company 2",
                companyKey = "e1c2"
        )

    }

    @Test
    @Throws(Exception::class)
    fun testCreateUser() {
        val connection = create(true)

        val newUser = createUser("Guy", "Inçögnítò")
        val pUser = ldapService.insert(connection, newUser)

        assertFalse(StringUtils.containsAny(pUser!!.gecos, 'ç', 'ö', 'í', 'ò'))

        Assert.assertEquals(pUser.uid, "guyinc")
        Assert.assertEquals(pUser.gecos, "Guy Incoegnito")
        Assert.assertEquals(pUser.mail, "guy.incoegnito@example.com")
        Assert.assertNotNull(pUser.uidNumber)
        Assert.assertNotNull(pUser.loginShell)
        assertFalse(pUser.loginShell!!.isEmpty())
        Assert.assertNotNull(pUser.homeDirectory)
        assertFalse(pUser.homeDirectory!!.isEmpty())

        Assert.assertNotNull(pUser.telephoneNumber)
        Assert.assertNotNull(pUser.mobile)
        assertTrue(pUser.telephoneNumber.isEmpty())
        assertTrue(pUser.mobile.isEmpty())

        connection.close()
    }

    @Test
    @Throws(Exception::class)
    fun testCreateUserUserNameExceeding() {
        val users = LinkedList<User>()
        for (i in 1..10) {
            users.add(
                    createUser("Maximilian", "Mustermann", "", i.toString() + ".test@example.com", ""))
        }
        val connection = create(true)
        var i = 0
        try {
            for (user in users) {
                ldapService.insert(connection, user)
                i++
            }
        } catch (be: BusinessException) {
            Assert.assertEquals(be.code, "user.create.usernames.exceeded")
        }

        log.debug("Username autosuggest exeeded after {} tries", i)
        connection.close()
    }

    private fun rnd(): String {
        return RandomStringUtils.randomAlphabetic(16)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateUserUniqueAttributes() {
        val connection = create(true)

        val emailCom = ".test@example.com"
        val firstName = rnd()
        val lastName = rnd()

        val newUser = createUser(firstName, lastName)
        val pUser = ldapService.insert(connection, newUser)

        try {
            ldapService.insert(connection, createUser(rnd(), rnd(), pUser!!.uid, rnd() + emailCom, rnd()))
            Assert.fail()
        } catch (be: BusinessException) {
            Assert.assertEquals(be.code, "user.create.username.alreadyUsed")
        }

        try {
            ldapService.insert(connection, createUser(rnd(), rnd(), rnd(), pUser!!.mail, rnd()))
            Assert.fail()
        } catch (be: BusinessException) {
            Assert.assertEquals(be.code, "user.mail.alreadyUsed")
        }

        try {
            ldapService.insert(connection,
                    createUser(rnd(), rnd(), rnd(), rnd() + emailCom, pUser!!.employeeNumber))
            Assert.fail()
        } catch (be: BusinessException) {
            Assert.assertEquals(be.code, "user.employeeNumber.alreadyUsed")
        }

        connection.close()
    }

    @Test(expected = Exception::class)
    fun testEmailPrefixAlreadyUsed() {
        listOf("duplicate.prefix@domain", "duplicate.prefix@subdomain.domain").forEach {
            ldapService.insert(create(true), createUser(email =  it))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testREsetPasswordAndSetNewPassword() {
        val connection = create(true)

        val newUser = createUser(rnd(), rnd())
        val pUser = ldapService.insert(connection, newUser)
        val initialPassword = ldapService.resetPassword(connection, pUser!!)

        assertTrue(StringUtils.isNotBlank(initialPassword))

        val currentPassword = currentUser!!.password
        val newPassword = rnd()
        val success: Boolean

        Assert.assertNotEquals(currentPassword, newPassword)

        success = ldapService.changePassword(connection, currentUser!!, newPassword)

        assertTrue(success)

        connection.close()
    }

    companion object {

        private val log = LoggerFactory.getLogger(LdapServiceTests::class.java)
    }

}
