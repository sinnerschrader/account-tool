package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapQueries
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.Encrypt
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.ModelMaping
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.logic.entity.UserInfo
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.unboundid.ldap.sdk.*
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import javax.annotation.Resource
import java.text.Normalizer
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

import com.unboundid.ldap.sdk.SearchRequest.ALL_OPERATIONAL_ATTRIBUTES
import com.unboundid.ldap.sdk.SearchRequest.ALL_USER_ATTRIBUTES
import java.text.MessageFormat
import java.util.Arrays.asList

@Service
class LdapService {

    private val listingsCache: Cache<String, List<String>>

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Value("\${domain.primary}")
    private lateinit var primaryDomain: String

    @Value("\${domain.secondary}")
    private lateinit var secondaryDomain: String

    @Value("\${user.smbIdPrefix}")
    private lateinit var smbIdPrefix: String

    @Value("\${user.sambaFlags}")
    private lateinit var sambaFlags: String

    @Value("\${user.homeDirPrefix}")
    private lateinit var homeDirPrefix: String

    @Value("\${user.loginShell}")
    private lateinit var loginShell: String

    @Value("\${user.appendCompanyOnDisplayName}")
    private var appendCompanyOnDisplayName = true

    @Resource(name = "userMapping")
    private lateinit var userMapping: ModelMaping<User>

    @Resource(name = "groupMapping")
    private lateinit var groupMapping: ModelMaping<Group>

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var cachedLdapService : CachedLdapService

    @Transient private var lastUserNumber: Int? = null

    init {
        this.listingsCache = CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build()
    }

    fun getUserCount(connection: LDAPConnection): Int {
        try {
            val searchResult = connection.search(ldapConfiguration.baseDN, SearchScope.SUB,
                LdapQueries.listAllUsers)
            return searchResult.entryCount
        } catch (e: Exception) {
            log.error("Could not fetch count of all users", e)
        }

        return 0
    }

    fun getUsers(connection: LDAPConnection, firstResult: Int, maxResults: Int): List<User> {
        try {
            val request = SearchRequest(ldapConfiguration.baseDN, SearchScope.SUB,
                LdapQueries.listAllUsers, ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)
            val searchResult = connection.search(request)
            val count = searchResult.entryCount
            val fr = Math.min(Math.max(0, firstResult), count)
            val mr = Math.min(fr + maxResults, count)
            return userMapping.map(searchResult.searchEntries.subList(fr, mr))
        } catch (e: Exception) {
            log.error("Could not fetch all users", e)
        }

        return emptyList()
    }

    private fun getListingFromCacheOrLdap(connection: LDAPConnection, cacheKey: String, attribute: String): List<String>? {
        var result = listingsCache.getIfPresent(cacheKey)
        if (result == null) {
            try {
                val tempResult = HashSet<String>()
                val searchResult = connection.search(ldapConfiguration.baseDN, SearchScope.SUB,
                        LdapQueries.listAllUsers, attribute)

                var value: String
                for (entry in searchResult.searchEntries) {
                    value = StringUtils.trimToEmpty(entry.getAttributeValue(attribute))
                    if (StringUtils.isNotBlank(value)) {
                        tempResult.add(value)
                    }
                }
                if (tempResult.size > 0) {
                    log.debug("Found {} entries for attribue {}", tempResult.size, attribute)
                    result = ArrayList(tempResult)
                    Collections.sort(result)
                    listingsCache.put(cacheKey, result)
                }
            } catch (le: LDAPException) {
                log.warn("Could not fetch employee types")
            }

        }
        return result
    }

    fun getEmployeeType(connection: LDAPConnection): List<String>? {
        return getListingFromCacheOrLdap(connection, "employeeTypes", "description")
    }

    fun getLocations(connection: LDAPConnection): List<String>? {
        return getListingFromCacheOrLdap(connection, "locations", "l")
    }

    fun getDepartments(connection: LDAPConnection): List<String>? {
        return getListingFromCacheOrLdap(connection, "departments", "ou")
    }

    fun getUserByUid(connection: LDAPConnection, uid: String): User? {
        try {
            val searchResult = connection.search(ldapConfiguration.baseDN, SearchScope.SUB,
                MessageFormat.format(LdapQueries.findUserByUid,uid), ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)
            if (searchResult.entryCount > 1) {
                val msg = "Found multiple entries for uid \"" + uid + "\""
                log.warn(msg)
                throw IllegalStateException(msg)
            }
            if (searchResult.entryCount < 1) {
                log.trace("Could not retrieve user by uid {} ", uid)
                return null
            }
            val entry = searchResult.searchEntries[0]
            if (userMapping.isCompatible(entry)) {
                return userMapping.map(entry)
            }
        } catch (e: Exception) {
            log.error("Could not retrieve user from user with uid " + uid, e)
        }

        return null
    }

    fun getGroupMembers(connection: LDAPConnection, group: Group): SortedSet<UserInfo> {
        return group.memberIds.mapNotNull { cachedLdapService.getGroupMember(connection, it) }.toSortedSet()
    }

    fun getGroupByCN(connection: LDAPConnection, groupCn: String?): Group? {
        try {
            val searchResult = connection.search(ldapConfiguration.groupDN, SearchScope.SUB,
                    MessageFormat.format(LdapQueries.findGroupByCn, groupCn))
            if (searchResult.entryCount > 1) {
                val msg = "Found multiple entries for group cn \"" + groupCn + "\""
                log.warn(msg)
                throw IllegalStateException(msg)
            }
            if (searchResult.entryCount < 1) {
                log.warn("Could not retrieve group by cn {} ", groupCn)
                return null
            }
            val entry = searchResult.searchEntries[0]
            if (groupMapping.isCompatible(entry)) {
                return groupMapping.map(entry)
            }
        } catch (e: Exception) {
            log.error("Could not retrieve group from ldap with cn " + groupCn, e)
        }

        return null
    }

    fun findUserBySearchTerm(connection: LDAPConnection, searchTerm: String): List<User> {
        val result = LinkedList<User>()
        try {
            val searchResult = connection.search(ldapConfiguration.baseDN,
                    SearchScope.SUB, MessageFormat.format(LdapQueries.searchUser, "*$searchTerm*"), ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)
            result.addAll(userMapping.map(searchResult.searchEntries))
            Collections.sort(result)
        } catch (e: Exception) {
            log.error("Could not find user by searchTermn " + searchTerm, e)
        }

        return result
    }

    fun getGroups(connection: LDAPConnection): List<Group> {
        val result = LinkedList<Group>()
        try {
            val searchResult = connection.search(ldapConfiguration.groupDN, SearchScope.SUB,
                LdapQueries.listAllGroups)
            result.addAll(groupMapping.map(searchResult.searchEntries))
            Collections.sort(result)
        } catch (e: Exception) {
            log.error("Could not retrieve groups from ldap ", e)
        }

        return result
    }

    fun getGroupsByUser(connection: LDAPConnection, uid: String, userDN: String): List<Group> {
        val result = LinkedList<Group>()
        try {
            val searchResult = connection.search(ldapConfiguration.groupDN, SearchScope.SUB,
                MessageFormat.format(LdapQueries.findGroupsByUser, uid, userDN))
            result.addAll(groupMapping.map(searchResult.searchEntries))
            Collections.sort(result)
        } catch (e: Exception) {
            log.error("Could not retrieve groups by user " + uid, e)
        }

        return result
    }

    private fun extractUidFromDN(uidOrDN: String): String {
        return if (StringUtils.startsWith(uidOrDN, "uid=")) {
            StringUtils.substring(uidOrDN, 4, uidOrDN.indexOf(','))
        } else uidOrDN
    }

    fun getUsersByGroup(connection: LDAPConnection, group: Group?): List<User> {
        val users = ArrayList<User>()
        try {
            group!!.memberIds.forEach { uidOrDN ->
                val user = getUserByUid(connection, extractUidFromDN(uidOrDN))
                if (user != null) {
                    users.add(user)
                }
            }
            Collections.sort(users)
        } catch (e: Exception) {
            log.error("Could not retrieve users by group " + group!!.cn, e)
        }

        return users
    }

    fun addUserToGroup(connection: LDAPConnection, user: User, group: Group): Group? {
        val ldapGroup = getGroupByCN(connection, group.cn) ?: return null
        if (ldapGroup.hasMember(user.uid, user.dn))
            return ldapGroup
        try {
            val groupType = ldapGroup.groupType
            val memberValue = if (groupType === Group.GroupType.Posix) user.uid else user.dn
            val memberAttribute = groupType.memberAttritube
            val modification = Modification(ModificationType.ADD, memberAttribute, memberValue)
            connection.modify(ldapGroup.dn, modification)
        } catch (le: LDAPException) {
            log.error("Could not add user {} to group {}", user.uid, group.cn)
            if (log.isDebugEnabled) {
                log.error("Could not add user", le)
            }
            return ldapGroup
        }

        return getGroupByCN(connection, group.cn)
    }

    fun removeUserFromGroup(connection: LDAPConnection, user: User, group: Group): Group? {
        val ldapGroup = getGroupByCN(connection, group.cn) ?: return null
        if (!ldapGroup.hasMember(user.uid, user.dn))
            return ldapGroup
        try {
            val groupType = ldapGroup.groupType
            val memberValue = if (groupType === Group.GroupType.Posix) user.uid else user.dn
            val memberAttribute = groupType.memberAttritube
            val modification = Modification(ModificationType.DELETE, memberAttribute, memberValue)
            connection.modify(ldapGroup.dn, modification)
        } catch (le: LDAPException) {
            log.error("Could not add user {} to group {}", user.uid, group.cn)
            if (log.isDebugEnabled) {
                log.error("Could not add user", le)
            }
            return ldapGroup
        }

        return getGroupByCN(connection, group.cn)
    }

    @Throws(BusinessException::class)
    fun resetPassword(connection: LDAPConnection, user: User): String? {
        return changePassword(connection, user, RandomStringUtils.randomAlphanumeric(32, 33))
    }

    @Throws(BusinessException::class)
    fun changePassword(connection: LDAPConnection, currentUser: LdapUserDetails, password: String): Boolean {
        val user = getUserByUid(connection, currentUser.username)
        val newPassword = changePassword(connection, user, password)
        if (newPassword != null) {
            currentUser.setPassword(newPassword)
        }
        return true
    }

    @Throws(BusinessException::class)
    private fun changePassword(connection: LDAPConnection, user: User?, newPassword: String): String? {
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val ldapUser = getUserByUid(connection, user!!.uid)

        val ldapNameGecos = ldapUser!!.gecos!!.toLowerCase().split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (StringUtils.containsIgnoreCase(newPassword, ldapUser.uid)
                || StringUtils.containsIgnoreCase(newPassword, ldapUser.sn)
                || StringUtils.containsIgnoreCase(newPassword, ldapUser.givenName)
                || StringUtils.containsAny(newPassword.toLowerCase(), *ldapNameGecos)) {
            throw BusinessException("Password can't contain user data.", "user.changePassword.failed")
        }

        val changes = ArrayList<Modification>()
        if (asList(*environment.activeProfiles).contains("development")) {
            log.warn("Plaintext crypter - dont use this on production")
            changes.add(Modification(ModificationType.REPLACE, "userPassword", StringUtils.trim(newPassword)))
        } else {
            changes.add(Modification(ModificationType.REPLACE, "userPassword", Encrypt.salt(newPassword)))
        }
        changes.add(Modification(ModificationType.REPLACE, "sambaNTPassword", Encrypt.samba(newPassword)))
        changes.add(Modification(ModificationType.REPLACE, "sambaPwdLastSet", timestamp))
        try {
            val result = connection.modify(ldapUser.dn, changes)
            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn("Could not properly change password for user {}. Reason: {} Status: {}",
                        ldapUser.uid, result.diagnosticMessage, result.resultCode)
            }
        } catch (le: LDAPException) {
            log.error("Could not change password for user {} ", user.uid)
            if (log.isDebugEnabled) {
                log.error("Could change password for user", le)
            }
            return null
        }

        return newPassword
    }

    fun activate(connection: LDAPConnection, user: User): Boolean {
        val (dn, uid, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, description, _, _, _, title) = getUserByUid(connection, user.uid) ?: return false

        val freelancerValues = arrayOf("Freelancer", "Feelancer")
        val changes = ArrayList<Modification>()

        changes.add(Modification(ModificationType.REPLACE, "szzStatus", "active"))
        changes.add(Modification(ModificationType.REPLACE, "szzMailStatus", "active"))
        if (StringUtils.equalsAny(title, *freelancerValues) || StringUtils.equalsAny(description, *freelancerValues)) {
            val exitDate = LocalDate.now().plusWeeks(4).plusDays(1)
            changes.add(Modification(ModificationType.REPLACE,
                    "szzExitDay", exitDate.dayOfMonth.toString()))
            changes.add(Modification(ModificationType.REPLACE,
                    "szzExitMonth", exitDate.monthValue.toString()))
            changes.add(Modification(ModificationType.REPLACE,
                    "szzExitYear", exitDate.year.toString()))
        }
        try {
            val result = connection.modify(dn, changes)
            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn("Could not properly activate user {}. Reason: {} Status: {}",
                        uid, result.diagnosticMessage, result.resultCode)
            }
        } catch (le: LDAPException) {
            log.error("Could not activate user {}. Reason: {}", uid, le.resultString)
            if (log.isDebugEnabled) {
                log.error("Could not activate user", le)
            }
            return false
        }

        return true
    }

    fun deactivate(connection: LDAPConnection, user: User): Boolean {
        val (dn, uid) = getUserByUid(connection, user.uid) ?: return false

        val changes = ArrayList<Modification>()
        changes.add(Modification(ModificationType.REPLACE, "szzStatus", "inactive"))
        changes.add(Modification(ModificationType.REPLACE, "szzMailStatus", "inactive"))
        try {
            val result = connection.modify(dn, changes)
            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn("Could not properly activate user {}. Reason: {} Status: {}",
                        uid, result.diagnosticMessage, result.resultCode)
                return false
            }
        } catch (le: LDAPException) {
            log.error("Could not activate user {}. Reason: {}", uid, le.resultString)
            if (log.isDebugEnabled) {
                log.error("Could not activate user", le)
            }
            return false
        }

        return false
    }

    private fun createMail(firstName: String, surname: String, domain: String?, shortFirstname: Boolean): String {
        val fn = mailify(firstName)
        val sn = mailify(surname)
        return StringUtils.join<String>(if (shortFirstname) fn[0].toString() else fn, ".", sn, "@", domain)
    }

    @Throws(BusinessException::class)
    private fun generateEmployeeID(connection: LDAPConnection): String {
        var i = 0
        var tmpEmployeeNumber = UUID.randomUUID().toString()
        while (isUserAttributeAlreadyUsed(connection, "employeeNumber", tmpEmployeeNumber)) {
            tmpEmployeeNumber = UUID.randomUUID().toString()
            i++
            if (i > 20) {
                throw BusinessException("Can't generate unique employee number after 20 retries.",
                        "user.employeeNumber.cantFindUnique")
            }
        }
        return tmpEmployeeNumber
    }

    @Throws(BusinessException::class)
    fun insert(connection: LDAPConnection, user: User): User? {
        try {
            var mailError = "alreadyUsed"
            var mail = user.mail
            if (StringUtils.isBlank(mail)) {
                mail = createMail(user.givenName, user.sn, primaryDomain, false)
                mailError = "autofillFailed"
            }
            if (isUserAttributeAlreadyUsed(connection, "mail", mail)) {
                throw BusinessException("E-Mail Address already used.", "user.mail." + mailError)
            }
            var tmpEmployeeNumber = user.employeeNumber
            if (StringUtils.isBlank(tmpEmployeeNumber)) {
                tmpEmployeeNumber = generateEmployeeID(connection)
            } else {
                if (isUserAttributeAlreadyUsed(connection, "employeeNumber", tmpEmployeeNumber)) {
                    throw BusinessException("The entered employeenumber is already in use",
                            "user.employeeNumber.alreadyUsed")
                }
            }

            val username = getUidSuggestion(connection, user.uid, user.givenName, user.sn)
            val dn = ldapConfiguration.getUserBind(username, user.companyKey)
            val fullName = user.givenName + " " + user.sn
            val displayName = fullName + " (" + user.companyKey.toLowerCase() + ")"
            val uidNumber = getNextUserID(connection)
            val password = RandomStringUtils.randomAlphanumeric(16, 33)
            val gidNumber = 100
            val homeDirectory = homeDirPrefix + username
            val employeeNumber = tmpEmployeeNumber
            val sambaTimestamp = System.currentTimeMillis() / 1000L
            val sambaSID = smbIdPrefix + (uidNumber * 2 + 1000)
            val sambaPWHistory = "0000000000000000000000000000000000000000000000000000000000000000"

            val attributes = ArrayList<Attribute>()

            // Default Values and LDAP specific entries
            attributes.add(Attribute("objectClass", User.objectClasses))
            attributes.add(Attribute("employeeNumber", employeeNumber))
            attributes.add(Attribute("uidNumber", uidNumber.toString()))
            attributes.add(Attribute("gidNumber", gidNumber.toString()))
            attributes.add(Attribute("loginShell", loginShell))
            attributes.add(Attribute("homeDirectory", homeDirectory))
            attributes.add(Attribute("sambaSID", sambaSID))
            attributes.add(Attribute("sambaAcctFlags", sambaFlags))
            attributes.add(Attribute("sambaPasswordHistory", sambaPWHistory))
            attributes.add(Attribute("sambaPwdLastSet", sambaTimestamp.toString()))
            attributes.add(Attribute("sambaNTPassword", Encrypt.samba(password)))
            attributes.add(Attribute("userPassword", Encrypt.salt(password)))
            //attributes.add(new Attribute("szzPublicKey", ""));

            // Person informations
            attributes.add(Attribute("uid", username))
            attributes.add(Attribute("givenName", user.givenName))
            attributes.add(Attribute("sn", user.sn))
            attributes.add(Attribute("cn", fullName))
            attributes.add(Attribute("displayName", if (appendCompanyOnDisplayName) displayName else fullName))
            attributes.add(Attribute("gecos", asciify(fullName)))

            // Organisational Entries
            attributes.add(Attribute("o", user.o))
            attributes.add(Attribute("ou", user.ou))
            attributes.add(Attribute("title", user.title))
            attributes.add(Attribute("l", user.l))
            attributes.add(Attribute("description", user.description))

            // Contact informations
            attributes.add(Attribute("mail", mail))
            if (StringUtils.isNotBlank(user.telephoneNumber)) {
                attributes.add(Attribute("telephoneNumber", user.telephoneNumber))
            }
            if (StringUtils.isNotBlank(user.mobile)) {
                attributes.add(Attribute("mobile", user.mobile))
            }

            // Birthday with Day and Month
            if (user.birthDate != null) {
                val birth = user.birthDate
                attributes.add(Attribute("szzBirthDay", birth.dayOfMonth.toString()))
                attributes.add(Attribute("szzBirthMonth", birth.monthValue.toString()))
            }

            // Entry Date
            val entry = user.employeeEntryDate ?: throw BusinessException("Entry could not be null", "user.entry.required")
            attributes.add(Attribute("szzEntryDay", entry.dayOfMonth.toString()))
            attributes.add(Attribute("szzEntryMonth", entry.monthValue.toString()))
            attributes.add(Attribute("szzEntryYear", entry.year.toString()))

            // Exit Date
            val exit = user.employeeExitDate ?: throw BusinessException("Exit could not be null", "user.exit.required")
            attributes.add(Attribute("szzExitDay", exit.dayOfMonth.toString()))
            attributes.add(Attribute("szzExitMonth", exit.monthValue.toString()))
            attributes.add(Attribute("szzExitYear", exit.year.toString()))

            // States
            attributes.add(Attribute("szzStatus", user.szzStatus.name))
            attributes.add(Attribute("szzMailStatus", user.szzMailStatus.name))

            val result = connection.add(dn, attributes)
            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn(
                        "Could not create new user with dn '{}' username '{}' and uidNumber '{}'. Reason: {} Status: {}",
                        dn, username, uidNumber, result.diagnosticMessage, result.resultCode)

                val args = arrayOf(result.resultString, result.resultCode.name, result.resultCode.intValue())
                throw BusinessException("LDAP rejected creation of user", "user.create.failed", args)
            }
            return getUserByUid(connection, username)
        } catch (le: LDAPException) {
            val msg = "Could not create user"
            log.error(msg)
            if (log.isDebugEnabled) {
                log.error(msg, le)
            }
            val args = arrayOf(le.resultString, le.resultCode.name, le.resultCode.intValue())
            throw BusinessException(msg, "user.create.failed", args, le)
        }

    }

    private fun asciify(value: String): String {
        val searchList = arrayOf("ä", "Ä", "ü", "Ü", "ö", "Ö", "ß")
        val replacementList = arrayOf("ae", "Ae", "ue", "Ue", "oe", "Oe", "ss")
        return Normalizer.normalize(
                StringUtils.replaceEach(StringUtils.trimToEmpty(value), searchList, replacementList),
                Normalizer.Form.NFD)
                .replace("\\p{M}".toRegex(), "")
    }

    private fun mailify(value: String): String {
        var temp = StringUtils.trimToEmpty(value)
        temp = StringUtils.replaceAll(temp, "\\.", "")
        temp = asciify(temp)
        temp = temp.toLowerCase()
        return StringUtils.replaceAll(temp, "[^a-z0-9]", "-")
    }

    /**
     * This method generated some suggestions for usernames. This suggestions can also handle short names with 1 or 2 characters.
     * Example: Viktor Gruber
     * Result into following variables:
     * fnBeginPart = vik
     * snBeginPart = gru
     * fnEndPart = tor
     * snEndPart = ber
     *
     *
     * Suggestions:
     * vik + gru
     * gru + vik
     * vik + ber
     * ber + vik
     * tor + gru
     * gru + tor
     * tor + ber
     * ber + tor
     *
     * @param firstname - firstnam of a person
     * @param surname   - lastname of a person
     * @return a set of suggestions for usernames
     */
    @Throws(BusinessException::class)
    private fun createUidSuggestions(firstname: String, surname: String): Set<String> {
        if (StringUtils.isBlank(firstname) || StringUtils.isBlank(surname)) {
            throw IllegalArgumentException("First- and Surname are now allowed to be null or empty")
        }
        if (firstname.length < 3 || surname.length <= 3) {
            throw BusinessException("Firstname and Lastname have to be at minimum 3 characters long",
                    "user.create.usernames.dontmatch")
        }
        val fn = mailify(firstname)
        val sn = mailify(surname)

        val res = LinkedHashSet<String>()
        var name = StringUtils.substring(fn + sn, 0, 3)
        name = name + sn
        name = StringUtils.substring(name, 0, 6)

        val fnBeginPart = StringUtils.substring(name, 0, 3)
        val snBeginPart = StringUtils.substring(name, 3)

        val pos = Math.max(Math.min(3, fn.length - 3), 0)
        name = StringUtils.substring(fn + sn, pos, pos + 3)
        name = name + StringUtils.reverse(StringUtils.substring(StringUtils.reverse(sn), 0, 3))

        val fnEndPart = StringUtils.substring(name, 0, 3)
        val snEndPart = StringUtils.substring(name, 3)

        res.add(fnBeginPart + snBeginPart)
        res.add(snBeginPart + fnBeginPart)

        res.add(fnBeginPart + snEndPart)
        res.add(snEndPart + fnBeginPart)

        res.add(fnEndPart + snBeginPart)
        res.add(snBeginPart + fnEndPart)

        res.add(fnEndPart + snEndPart)
        res.add(snEndPart + fnEndPart)

        return res
    }

    @Throws(BusinessException::class)
    private fun getUidSuggestion(connection: LDAPConnection, username: String, firstName: String, lastName: String): String {
        if (StringUtils.isNotBlank(username)) {
            if (getUserByUid(connection, username) != null) {
                throw BusinessException("Entered username is already used.", "user.create.username.alreadyUsed")
            }
            return username
        }
        val uidSuggestions = createUidSuggestions(firstName, lastName)
        for (uidSuggestion in uidSuggestions) {
            if (getUserByUid(connection, uidSuggestion) == null) {
                return uidSuggestion
            }
        }
        throw BusinessException("Could not create username, all suggestions are already used",
                "user.create.usernames.exceeded")
    }

    @Throws(BusinessException::class)
    fun update(connection: LDAPConnection, user: User): User? {
        val (currentDN, uid, _, _, _, _, _, _, _, _, _, birthDate, _, _, _, _, szzStatus, szzMailStatus, _, employeeEntryDate, employeeExitDate, ou, description, telephoneNumber, mobile, employeeNumber1, title, l, szzPublicKey, _, companyKey) = getUserByUid(connection, user.uid) ?: throw BusinessException("The modification was called for a non existing user", "user.notExists")
        try {
            var modifyDNRequest: ModifyDNRequest? = null
            val changes = ArrayList<Modification>()
            if (!StringUtils.equals(companyKey, user.companyKey)) {
                val delete = true
                val newDN = ldapConfiguration.getUserBind(user.uid, user.companyKey)
                val newRDN = StringUtils.split(newDN, ",")[0]
                val superiorDN = newDN.replace(newRDN + ",", StringUtils.EMPTY)

                log.warn("Move user to other company. From: {} To: {} + {}", currentDN, newRDN, superiorDN)
                modifyDNRequest = ModifyDNRequest(currentDN, newRDN, delete, superiorDN)

                changes.add(Modification(ModificationType.REPLACE, "o", user.o))
            }

            // Default Values and LDAP specific entries
            if (isChanged(user.employeeNumber, employeeNumber1, true)) {
                var employeeNumber = user.employeeNumber
                if (StringUtils.isBlank(employeeNumber)) {
                    employeeNumber = generateEmployeeID(connection)
                } else if (isUserAttributeAlreadyUsed(connection, "employeeNumber", user.employeeNumber)) {
                    throw BusinessException("Entered Employeenumber already used",
                            "user.modify.employeeNumber.alreadyUsed")
                }
                changes.add(Modification(ModificationType.REPLACE, "employeeNumber", employeeNumber))
            }
            if (isChanged(user.szzPublicKey, szzPublicKey)) {
                changes.add(Modification(ModificationType.REPLACE, "szzPublicKey", user.szzPublicKey!!))
            }

            // Organisational Entries
            if (isChanged(user.ou, ou)) {
                changes.add(Modification(ModificationType.REPLACE, "ou", user.ou))
            }
            if (isChanged(user.title, title)) {
                changes.add(Modification(ModificationType.REPLACE, "title", user.title))
            }
            if (isChanged(user.l, l)) {
                changes.add(Modification(ModificationType.REPLACE, "l", user.l))
            }
            if (isChanged(user.description, description)) {
                changes.add(Modification(ModificationType.REPLACE, "description", user.description))
            }

            // Contact informations
            if (isChanged(user.telephoneNumber, telephoneNumber, true)) {
                if (StringUtils.isBlank(user.telephoneNumber)) {
                    changes.add(Modification(ModificationType.DELETE, "telephoneNumber"))
                } else {
                    changes.add(Modification(ModificationType.REPLACE,
                            "telephoneNumber", user.telephoneNumber))
                }
            }
            if (isChanged(user.mobile, mobile, true)) {
                if (StringUtils.isBlank(user.mobile)) {
                    changes.add(Modification(ModificationType.DELETE, "mobile"))
                } else {
                    changes.add(Modification(ModificationType.REPLACE, "mobile", user.mobile))
                }
            }

            // Birthday with Day and Month
            val birth = user.birthDate
            if (birth != null && isChanged(birth, birthDate)) {
                changes.add(Modification(ModificationType.REPLACE,
                        "szzBirthDay", birth.dayOfMonth.toString()))
                changes.add(Modification(ModificationType.REPLACE,
                        "szzBirthMonth", birth.monthValue.toString()))
            } else if (birth == null && birthDate != null) {
                changes.add(Modification(ModificationType.DELETE, "szzBirthDay"))
                changes.add(Modification(ModificationType.DELETE, "szzBirthMonth"))
            }

            // Entry Date
            val entry = user.employeeEntryDate
            if (entry != null && isChanged(entry, employeeEntryDate)) {
                changes.add(Modification(ModificationType.REPLACE,
                        "szzEntryDay", entry.dayOfMonth.toString()))
                changes.add(Modification(ModificationType.REPLACE,
                        "szzEntryMonth", entry.monthValue.toString()))
                changes.add(Modification(ModificationType.REPLACE,
                        "szzEntryYear", entry.year.toString()))
            }

            // Exit Date
            val exit = user.employeeExitDate
            if (exit != null && isChanged(exit, employeeExitDate)) {
                changes.add(Modification(ModificationType.REPLACE,
                        "szzExitDay", exit.dayOfMonth.toString()))
                changes.add(Modification(ModificationType.REPLACE,
                        "szzExitMonth", exit.monthValue.toString()))
                changes.add(Modification(ModificationType.REPLACE,
                        "szzExitYear", exit.year.toString()))
            }

            // States
            if (isChanged(user.szzStatus, szzStatus) && user.szzStatus !== User.State.undefined) {
                changes.add(Modification(ModificationType.REPLACE,
                        "szzStatus", user.szzStatus.name))
            }
            if (isChanged(user.szzMailStatus, szzMailStatus) && user.szzMailStatus !== User.State.undefined) {
                changes.add(Modification(ModificationType.REPLACE,
                        "szzMailStatus", user.szzMailStatus.name))
            }

            var result: LDAPResult? = null
            // save modifications
            if (!changes.isEmpty()) {
                result = connection.modify(currentDN, changes)
                if (result!!.resultCode !== ResultCode.SUCCESS) {
                    log.warn("Could not modify user with dn '{}' username '{}'. Reason: {} Status: {}",
                            currentDN, uid, result!!.diagnosticMessage, result.resultCode)

                    val args = arrayOf(result.resultString, result.resultCode.name, result.resultCode.intValue())
                    throw BusinessException("LDAP rejected update of user", "user.modify.failed", args)
                }
            }
            // move user to other DN (Company)
            if (modifyDNRequest != null) {
                result = connection.modifyDN(modifyDNRequest)
                if (result!!.resultCode !== ResultCode.SUCCESS) {
                    log.warn("Could move user to other Company '{}' username '{}'. Reason: {} Status: {}",
                            currentDN, uid, result!!.diagnosticMessage, result.resultCode)

                    val args = arrayOf(result.resultString, result.resultCode.name, result.resultCode.intValue())
                    throw BusinessException("LDAP rejected update of user", "user.modify.failed", args)
                }
            }
        } catch (le: LDAPException) {
            val msg = "Could not change user"
            log.error(msg)
            if (log.isDebugEnabled) {
                log.error(msg, le)
            }
            val args = arrayOf(le.resultString, le.resultCode.name, le.resultCode.intValue())
            throw BusinessException(msg, "user.modify.failed", args, le)
        }

        return getUserByUid(connection, user.uid)
    }

    private fun fetchMaxUserIDNumber(connection: LDAPConnection): Int? {
        val attribute = "uidNumber"
        var result: Int = 1000
        try {
            val searchResult = connection.search(ldapConfiguration.baseDN, SearchScope.SUB,
                LdapQueries.listAllUsers, attribute)

            var uidNumber: Int?
            for (entry in searchResult.searchEntries) {
                uidNumber = entry.getAttributeValueAsInteger(attribute)
                if (uidNumber > result) {
                    result = uidNumber
                }
            }
        } catch (le: LDAPException) {
            log.warn("Could not fetch employee types")
        }

        return result
    }

    @Synchronized
    @Throws(BusinessException::class)
    private fun getNextUserID(connection: LDAPConnection): Int {
        if (lastUserNumber == null) {
            lastUserNumber = fetchMaxUserIDNumber(connection)
        }
        val maxTriesForNextUidNumber = 1000
        val maxUserNumber = lastUserNumber!! + maxTriesForNextUidNumber
        for (uidNumber in lastUserNumber!! + 1..maxUserNumber - 1) {
            try {
                val searchResult = connection.search(ldapConfiguration.baseDN, SearchScope.SUB,
                    MessageFormat.format(LdapQueries.findUserByUidNumber, uidNumber.toString()))
                if (searchResult.entryCount == 0) {
                    lastUserNumber = uidNumber
                    return uidNumber
                }
            } catch (le: LDAPException) {
                log.error("Could not fetch next uidNumber for new user, Reason: {}, result: {}",
                        le.diagnosticMessage, le.resultString)
                if (log.isDebugEnabled) {
                    log.error("Could not fetch next uidNumber for new user", le)
                }
            }

        }
        throw BusinessException("Could not find a valid new uid Number.", "uidNumber.exceeded")
    }

    private fun isChanged(newValue: Any?, originalValue: Any?, removeable: Boolean = false): Boolean {
        return (removeable || newValue != null && newValue != "") && newValue != originalValue
    }

    @Throws(BusinessException::class)
    private fun isUserAttributeAlreadyUsed(connection: LDAPConnection, attribute: String, value: String): Boolean {
        try {
            val result = connection.search(ldapConfiguration.baseDN, SearchScope.SUB,
                MessageFormat.format(LdapQueries.checkUniqAttribute, attribute, value), attribute)
            if (result.resultCode === ResultCode.SUCCESS) {
                return result.entryCount != 0
            }
            throw BusinessException("Could not check attribute",
                    "user.uniqAttributeCheck", arrayOf<Any>(attribute))
        } catch (le: LDAPException) {
            throw BusinessException("Could not check attribute",
                    "general.ldap.failed", arrayOf<Any>(le.diagnosticMessage))
        }

    }

    fun getAdminGroup(connection: LDAPConnection, group: Group): Group? {
        if (group.isAdminGroup) {
            return group
        }

        val gp = ldapConfiguration.groupPrefixes
        val adminGroupCN = StringUtils.replace(group.cn, gp.team, gp.admin)

        val result = getGroupByCN(connection, adminGroupCN)
        return if (result != null && result.isAdminGroup) {
            result
        } else getGroupByCN(connection, ldapConfiguration.permissions.ldapAdminGroup)
    }

    fun getGroupAdmins(connection: LDAPConnection, group: Group): List<User> {
        return getUsersByGroup(connection, getAdminGroup(connection, group))
    }

    companion object {

        private val log = LoggerFactory.getLogger(LdapService::class.java)
    }

}
