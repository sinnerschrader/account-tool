package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.sinnerschrader.s2b.accounttool.config.DomainConfiguration
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapQueries
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.Encrypt
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.GroupMapping
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.UserMapping
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.logic.entity.Group.GroupType.Posix
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State.active
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State.inactive
import com.sinnerschrader.s2b.accounttool.logic.entity.UserInfo
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.unboundid.ldap.sdk.*
import com.unboundid.ldap.sdk.ModificationType.*
import com.unboundid.ldap.sdk.SearchRequest.ALL_OPERATIONAL_ATTRIBUTES
import com.unboundid.ldap.sdk.SearchRequest.ALL_USER_ATTRIBUTES
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.security.GeneralSecurityException
import java.text.MessageFormat
import java.text.Normalizer
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Arrays.asList
import java.util.concurrent.TimeUnit

@Service
class LdapService {
    private val listingsCache: Cache<String, List<String>> = CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build()

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var domainConfiguration: DomainConfiguration

    @Value("\${user.smbIdPrefix}")
    private lateinit var smbIdPrefix: String

    @Value("\${user.homeDirPrefix}")
    private lateinit var homeDirPrefix: String

    @Autowired
    private lateinit var userMapping: UserMapping

    @Autowired
    private lateinit var groupMapping: GroupMapping

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var cachedLdapService: CachedLdapService

    private var lastUserNumber: Int? = null

    @Autowired
    private lateinit var managementConfiguration: LdapManagementConfiguration

    fun getUserCount(connection: LDAPConnection) =
        try {
            connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB, LdapQueries.listAllUsers).entryCount
        } catch (e: Exception) {
            log.error("Could not fetch count of all users", e)
            0
        }

    fun getUsers(connection: LDAPConnection, firstResult: Int, maxResults: Int): List<User> {
        try {
            val request = SearchRequest(ldapConfiguration.config.baseDN, SearchScope.SUB,
                    LdapQueries.listAllUsers, ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)
            val searchResult = connection.search(request)
            val count = searchResult.entryCount
            val fr = Math.min(Math.max(0, firstResult), count)
            val mr = Math.min(fr + maxResults, count)

            return searchResult.searchEntries.subList(fr, mr).mapNotNull {
                userMapping.map(it)
            }.sorted()

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
                val searchResult = connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB,
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

    fun getEmployeeType(connection: LDAPConnection) = getListingFromCacheOrLdap(connection, "employeeTypes", "description")

    fun getLocations(connection: LDAPConnection) = getListingFromCacheOrLdap(connection, "locations", "l")

    fun getDepartments(connection: LDAPConnection) = getListingFromCacheOrLdap(connection, "departments", "ou")

    fun getUserByUid(connection: LDAPConnection, uid: String): User? {
        try {
            val searchResult = connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB,
                    MessageFormat.format(LdapQueries.findUserByUid, uid), ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)
            if (searchResult.entryCount > 1) {
                val msg = "Found multiple entries for uid \"$uid\""
                log.warn(msg)
                throw IllegalStateException(msg)
            }
            if (searchResult.entryCount < 1) {
                log.trace("Could not retrieve user by uid {} ", uid)
                return null
            }
            val entry = searchResult.searchEntries[0]
            return userMapping.map(entry)
        } catch (e: Exception) {
            log.error("Could not retrieve user from user with uid $uid", e)
        }

        return null
    }

    fun getGroupMembers(connection: LDAPConnection, group: Group): SortedSet<UserInfo> {
        return group.memberIds.mapNotNull { cachedLdapService.getGroupMember(connection, it) }.toSortedSet()
    }

    fun getGroupByCN(connection: LDAPConnection, groupCn: String?): Group? {
        try {
            val searchResult = connection.search(ldapConfiguration.config.groupDN, SearchScope.SUB,
                    MessageFormat.format(LdapQueries.findGroupByCn, groupCn))
            if (searchResult.entryCount > 1) {
                val msg = "Found multiple entries for group cn \"$groupCn\""
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
            log.error("Could not retrieve group from ldap with cn $groupCn", e)
        }

        return null
    }

    fun findUserBySearchTerm(connection: LDAPConnection, searchTerm: String): List<User> {
        try {
            val searchResult = connection.search(ldapConfiguration.config.baseDN,
                    SearchScope.SUB, MessageFormat.format(LdapQueries.searchUser, "*$searchTerm*"), ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)

            return searchResult.searchEntries.mapNotNull {
                userMapping.map(it)
            }.sorted()
        } catch (e: Exception) {
            log.error("Could not find user by searchTermn $searchTerm", e)
        }
        return emptyList()
    }

    fun getGroups(connection: LDAPConnection): List<Group> {
        val result = LinkedList<Group>()
        try {
            val searchResult = connection.search(ldapConfiguration.config.groupDN, SearchScope.SUB,
                    LdapQueries.listAllGroups)
            result.addAll(groupMapping.map(searchResult.searchEntries))
            result.sort()
        } catch (e: Exception) {
            log.error("Could not retrieve groups from ldap ", e)
        }

        return result
    }

    fun getGroupsByUser(connection: LDAPConnection, uid: String, userDN: String): List<Group> {
        val result = LinkedList<Group>()
        try {
            val searchResult = connection.search(ldapConfiguration.config.groupDN, SearchScope.SUB,
                    MessageFormat.format(LdapQueries.findGroupsByUser, uid, userDN))
            result.addAll(groupMapping.map(searchResult.searchEntries))
            result.sort()
        } catch (e: Exception) {
            log.error("Could not retrieve groups by user $uid", e)
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
            users.sort()
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
            val memberValue = if (groupType === Posix) user.uid else user.dn
            val memberAttribute = groupType.memberAttritube
            val modification = Modification(ADD, memberAttribute, memberValue)
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
            val memberValue = if (groupType === Posix) user.uid else user.dn
            val memberAttribute = groupType.memberAttritube
            val modification = Modification(DELETE, memberAttribute, memberValue)
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
        val ldapUser = getUserByUid(connection, user!!.uid)!!

        val changes = ArrayList<Modification>()
        if (asList(*environment.activeProfiles).contains("development")) {
            log.warn("Plaintext crypter - dont use this on production")
            changes.add(Modification(REPLACE, "userPassword", StringUtils.trim(newPassword)))
        } else {
            changes.add(Modification(REPLACE, "userPassword", Encrypt.salt(newPassword)))
        }
        changes.add(Modification(REPLACE, "sambaNTPassword", Encrypt.samba(newPassword)))
        changes.add(Modification(REPLACE, "sambaPwdLastSet", timestamp))
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

    fun changeUserState(connection: LDAPConnection, uid: String, state: User.State): Boolean {
        val user = getUserByUid(connection, uid) ?: return false
        try {
            val result = connection.modify(user.dn, mapOf(
                    "szzStatus" to state.name,
                    "szzMailStatus" to state.name
            ).toModification())

            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn("Could not change user $uid to $state. Reason: ${result.diagnosticMessage} Status: ${result.resultCode}")
                return false
            }
        } catch (le: LDAPException) {
            log.error("Could not activate user $uid. Reason: ${le.resultString}")
            return false
        }
        return true
    }

    @CacheEvict("groupMembers", key = "#uid")
    fun activate(connection: LDAPConnection, uid: String) = changeUserState(connection, uid, active)

    @CacheEvict("groupMembers", key = "#uid")
    fun deactivate(connection: LDAPConnection, uid: String) = changeUserState(connection, uid, inactive)


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
    @CacheEvict("groupMembers", key = "#user.uid")
    fun insert(connection: LDAPConnection, user: User): User? {
        try {
            val nextUserID = getNextUserID(connection)
            val uidSuggestion = getUidSuggestion(connection, user.uid, user.givenName, user.sn)
            val userWithDefaults = user.copy(
                    dn = ldapConfiguration.getUserBind(uidSuggestion, user.companyKey),
                    uid = uidSuggestion,
                    uidNumber = nextUserID,
                    mail = if (user.mail.isNotBlank()) user.mail else createMail(user.givenName, user.sn, domainConfiguration.mailDomain(user.description), false),
                    homeDirectory = homeDirPrefix + user.uid,
                    sambaSID = smbIdPrefix + (nextUserID * 2 + 1000),
                    employeeNumber = if (user.employeeNumber.isNotBlank()) user.employeeNumber else generateEmployeeID(connection),
                    displayName = "${user.givenName} ${user.sn} (${user.companyKey.toLowerCase()})"
            )

            if (isEmailPrefixAlreadyUsed(connection, userWithDefaults.mail))
                throw BusinessException("Email prefix is already used.", "user.mail.alreadyUsed", arrayOf<Any>(userWithDefaults.mail.substringBefore("@")))
            if (isUserAttributeAlreadyUsed(connection, "employeeNumber", userWithDefaults.employeeNumber))
                throw BusinessException("The entered employeenumber is already in use", "user.employeeNumber.alreadyUsed")
            userWithDefaults.szzEntryDate ?: throw BusinessException("Entry could not be null", "user.entry.required")
            userWithDefaults.szzExitDate ?: throw BusinessException("Exit could not be null", "user.exit.required")

            val result = connection.add(userWithDefaults.dn, userWithDefaults.toAttributes())
            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn("Could not create new user with dn '${userWithDefaults.dn}' username '${userWithDefaults.uid}' and uidNumber '${userWithDefaults.uidNumber}'. Reason: ${result.diagnosticMessage} Status: ${result.resultCode}")
                throw BusinessException("LDAP rejected creation of user", "user.create.failed", arrayOf(result.resultString, result.resultCode.name, result.resultCode.intValue()))
            }
            resetPassword(connection, userWithDefaults)
            return getUserByUid(connection, userWithDefaults.uid)
        } catch (le: LDAPException) {
            log.error("Could not create user: ${le.message}")
            throw BusinessException("Could not create user", "user.create.failed", arrayOf(le.resultString, le.resultCode.name, le.resultCode.intValue()), le)
        }

    }

    private fun isEmailPrefixAlreadyUsed(connection: LDAPConnection, mail: String) =
            isUserAttributeAlreadyUsed(connection, "mail", "${mail.substringBefore("@")}@*")

    fun Map<String, Any>.toModification(modificationType: ModificationType = REPLACE) = this.map { entry ->
        entry.value.let {
            when (it) {
                is String -> Modification(modificationType, entry.key, it)
                is Number -> Modification(modificationType, entry.key, it.toString())
                else -> throw UnsupportedOperationException()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun User.toMap() = OBJECT_MAPPER.convertValue(this, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    fun User.toAttributes() = toMap().map {
        // TODO cleanup types (generics)
        val k = it.key as String
        val v = it.value
        when (v) {
            is Number -> Attribute(k, v.toString())
            is String -> Attribute(k, v)
            is Collection<*> -> Attribute(k, v as Collection<String>)
            else -> throw UnsupportedOperationException()
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
        name += sn
        name = StringUtils.substring(name, 0, 6)

        val fnBeginPart = StringUtils.substring(name, 0, 3)
        val snBeginPart = StringUtils.substring(name, 3)

        val pos = Math.max(Math.min(3, fn.length - 3), 0)
        name = StringUtils.substring(fn + sn, pos, pos + 3)
        name += StringUtils.reverse(StringUtils.substring(StringUtils.reverse(sn), 0, 3))

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

    @CacheEvict("groupMembers", key = "#user.uid")
    fun update(connection: LDAPConnection, user: User): User? {
        val pUser = getUserByUid(connection, user.uid)
                ?: throw BusinessException("The modification was called for a non existing user", "user.notExists")
        try {
            var modifyDNRequest: ModifyDNRequest? = null
            val changes = ArrayList<Modification>()
            if (!StringUtils.equals(pUser.companyKey, user.companyKey)) {
                val delete = true
                val newDN = ldapConfiguration.getUserBind(user.uid, user.companyKey)
                val newRDN = StringUtils.split(newDN, ",")[0]
                val superiorDN = newDN.replace("$newRDN,", StringUtils.EMPTY)

                log.warn("Move user to other company. From: {} To: {} + {}", pUser.dn, newRDN, superiorDN)
                modifyDNRequest = ModifyDNRequest(pUser.dn,
                        newRDN, delete, superiorDN)
            }

            // Default Values and LDAP specific entries
            if (isChanged(user.employeeNumber, pUser.employeeNumber, true)) {
                var employeeNumber = user.employeeNumber
                if (StringUtils.isBlank(employeeNumber)) {
                    employeeNumber = generateEmployeeID(connection)
                } else if (isUserAttributeAlreadyUsed(connection, "employeeNumber", user.employeeNumber)) {
                    throw BusinessException("Entered Employeenumber already used",
                            "user.modify.employeeNumber.alreadyUsed")
                }
                changes.add(Modification(REPLACE, "employeeNumber", employeeNumber))
            }

            val changedEntries = with(pUser.toMap()){
                user.toMap().filter { it.value != this[it.key] }
            } - listOf("uidNumber", "displayName", "homeDirectory", "sambaSID", "mail") // TODO changes to these currently not detected/supported
            changes.addAll(changedEntries.toModification())

            var result: LDAPResult?
            if (!changes.isEmpty()) {
                result = connection.modify(pUser.dn, changes)
                if (result!!.resultCode !== ResultCode.SUCCESS) {
                    log.warn("Could not modify user with dn '{}' username '{}'. Reason: {} Status: {}",
                            pUser.dn, pUser.uid, result!!.diagnosticMessage, result.resultCode)
                    throw BusinessException("LDAP rejected update of user", "user.modify.failed",
                            arrayOf(result.resultString, result.resultCode.name, result.resultCode.intValue()))
                }
            }
            // move user to other DN (Company)
            if (modifyDNRequest != null) {
                result = connection.modifyDN(modifyDNRequest)
                if (result!!.resultCode !== ResultCode.SUCCESS) {
                    log.warn("Could move user to other Company '{}' username '{}'. Reason: {} Status: {}",
                            pUser.dn, pUser.uid, result!!.diagnosticMessage, result.resultCode)

                    val args = arrayOf(result.resultString, result.resultCode.name, result.resultCode.intValue())
                    throw BusinessException("LDAP rejected update of user", "user.modify.failed", args)
                }
            }

            if (isChanged(user.description, pUser.description)) {
                clearGroups(user, onlyDefaultGroups = user.szzStatus == active)
                if (user.szzStatus == active) addDefaultGroups(user)
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

    fun addDefaultGroups(user: User) {
        val defaultGroups = ldapConfiguration.permissions.defaultGroups[user.description] ?: emptyList()
        if (defaultGroups.isEmpty()) {
            log.debug("No default groups defined, skipped adding user to default groups")
            return
        }
        try {
            ldapConfiguration.createConnection().use { connection ->
                connection.bind(managementConfiguration.user.bindDN,
                        managementConfiguration.user.password)

                defaultGroups.forEach { groupCn ->
                    getGroupByCN(connection, groupCn)?.let {
                        addUserToGroup(connection, user, it)
                    }
                }
                log.debug("Added ${user.uid} to $defaultGroups")
            }
        } catch (e: LDAPException) {
            log.error("Could not add user to default groups", e)
        } catch (e: GeneralSecurityException) {
            log.error("Could not add user to default groups", e)
        }

    }

    fun clearGroups(user: User, onlyDefaultGroups: Boolean = false) {
        try {
            ldapConfiguration.createConnection().use { connection ->
                connection.bind(managementConfiguration.user.bindDN,
                        managementConfiguration.user.password)

                val defaultGroups = ldapConfiguration.permissions.defaultGroups.values.flatten().toSet()
                val removedGroups = getGroupsByUser(connection, user.uid, user.dn)
                        .filter { !onlyDefaultGroups || defaultGroups.contains(it.cn) }
                        .mapNotNull { removeUserFromGroup(connection, user, it) }
                log.debug("Removed ${user.uid} from ${removedGroups.map { it.cn }}")
            }
        } catch (e: LDAPException) {
            log.error("Could not clear groups for user ${user.uid}", e)
        } catch (e: GeneralSecurityException) {
            log.error("Could not clear groups for user ${user.uid}", e)
        }
    }

    private fun fetchMaxUserIDNumber(connection: LDAPConnection): Int? {
        val attribute = "uidNumber"
        var result: Int = 1000
        try {
            val searchResult = connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB,
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
        for (uidNumber in lastUserNumber!! + 1 until maxUserNumber) {
            try {
                val searchResult = connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB,
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
            val result = connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB,
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
        val OBJECT_MAPPER = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }

}
