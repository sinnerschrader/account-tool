package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sinnerschrader.s2b.accounttool.config.DomainConfiguration
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapQueries
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.Encrypt
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.GroupMapping
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.UserMapping
import com.sinnerschrader.s2b.accounttool.logic.entity.*
import com.sinnerschrader.s2b.accounttool.logic.entity.Group.GroupClassification.ADMIN
import com.sinnerschrader.s2b.accounttool.logic.entity.Group.GroupType.Posix
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State.active
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException
import com.unboundid.ldap.sdk.*
import com.unboundid.ldap.sdk.Filter.createANDFilter
import com.unboundid.ldap.sdk.Filter.createEqualityFilter
import com.unboundid.ldap.sdk.ModificationType.*
import com.unboundid.ldap.sdk.SearchRequest.ALL_OPERATIONAL_ATTRIBUTES
import com.unboundid.ldap.sdk.SearchRequest.ALL_USER_ATTRIBUTES
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.apache.commons.lang3.StringUtils
import org.glowroot.agent.api.Instrumentation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.security.GeneralSecurityException
import java.text.Normalizer
import java.util.*
import java.util.Arrays.asList

@Service
class LdapService {
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
    private lateinit var cachedLdapService: LdapService

    private var lastUserNumber: Int? = null

    @Autowired
    private lateinit var managementConfiguration: LdapManagementConfiguration

    @Instrumentation.Timer("getGroupMembers")
    @Cacheable("groupMembers", key = "#uid", unless = "#result == null")
    fun getGroupMember(connection: LDAPConnection, uid: String): UserInfo {
        try {
            val searchResult = connection.search(
                    ldapConfiguration.config.baseDN,
                    SearchScope.SUB,
                    createANDFilter(
                            createEqualityFilter("objectclass", "posixAccount"),
                            createEqualityFilter("uid", uid)
                    ),
                    "uid", "givenName", "sn", "mail", "szzStatus"
            )

            return when (searchResult.searchEntries.size) {
                0 -> UserInfo("UNKNOWN", uid, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", User.State.inactive, "UNKNOWN")
                1 -> with(searchResult.searchEntries.first()) {
                    UserInfo(
                            dn = dn,
                            uid = getAttributeValue("uid"),
                            givenName = getAttributeValue("givenName"),
                            sn = getAttributeValue("sn"),
                            o = companyForDn(dn),
                            mail = getAttributeValue("mail"),
                            szzStatus = User.State.valueOf(getAttributeValue("szzStatus")),
                            type = getAttributeValue("description") ?: "")
                }
                else -> throw IllegalStateException()
            }
        } catch (e: Exception) {
            throw RuntimeException("Could retrieve user [uid: $uid]", e)
        }
    }

    @Instrumentation.Timer("getGroupByCN")
    @Cacheable("groups", key = "#cn", unless = "#result == null")
    fun getGroupByCN(connection: LDAPConnection, cn: String) =
            try {
                val searchResult = connection.search(ldapConfiguration.config.groupDN, SearchScope.SUB,
                        LdapQueries.findGroupByCn(cn))

                when (searchResult.entryCount) {
                    1 -> groupMapping.map(searchResult.searchEntries[0])!!
                    0 -> throw RuntimeException("Could not retrieve group by cn $cn")
                    else -> throw IllegalStateException("Found multiple entries for group cn $cn")
                }
            } catch (e: Exception) {
                throw RuntimeException("Could not retrieve group from ldap with cn $cn", e)
            }

    fun companyForDn(dn: String) =
            try {
                with(Regex(",ou=([^,]+)").findAll(dn).last().groupValues[1]) {
                    ldapConfiguration.companies[this] ?: "UNKNOWN"
                }
            } catch (e: NoSuchElementException) {
                "UNKNOWN"
            }

    @Cacheable("uids", key = "'uids'") // TODO update on add
    @Instrumentation.Timer("getUserIDs")
    fun getUserIDs(connection: LDAPConnection) =
            try {
                with(connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB, LdapQueries.listAllUsers, "uid")) {
                    searchEntries.mapNotNull {
                        it.getAttributeValue("uid")
                    }.toSortedSet()
                }
            } catch (e: Exception) {
                throw RuntimeException("Could not fetch count of all users", e)
            }

    @Instrumentation.Timer("getUsers")
    fun getUsers(connection: LDAPConnection) =
            try {
                cachedLdapService.getUserIDs(connection).mapNotNull {
                    cachedLdapService.getUserByUid(connection, it)
                }.sorted()
            } catch (e: Exception) {
                log.error("Could not fetch all users", e)
                emptyList<User>()
            }

    @Instrumentation.Timer("getListing")
    @Cacheable("listing", key = "#attribute")
    fun getListing(connection: LDAPConnection, attribute: String) =
            try {
                connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB, LdapQueries.listAllUsers, attribute)
                        .searchEntries.map {
                    it.getAttributeValue(attribute).trim()
                }.filter { it.isNotBlank() }.toSortedSet()
            } catch (le: LDAPException) {
                log.warn("Could not fetch employee types")
                throw le
            }

    fun getEmployeeType(connection: LDAPConnection) = cachedLdapService.getListing(connection, "description")

    fun getLocations(connection: LDAPConnection) = cachedLdapService.getListing(connection, "l")

    fun getDepartments(connection: LDAPConnection) = cachedLdapService.getListing(connection, "ou")

    @Instrumentation.TraceEntry(message = "getUserByUid: {{1}}", timer = "getUserByUid")
    @Cacheable("users", key = "#uid", unless = "#result == null", condition = "#skipCache == false")
    @CacheEvict("users", key = "#uid", beforeInvocation = true, condition = "#skipCache")
    fun getUserByUid(connection: LDAPConnection, uid: String, skipCache: Boolean = false) =
            try {
                with(connection.search(ldapConfiguration.config.baseDN, SearchScope.SUB,
                        LdapQueries.findUserByUid(uid), ALL_USER_ATTRIBUTES, ALL_OPERATIONAL_ATTRIBUTES)) {
                    when (entryCount) {
                        1 -> userMapping.map(searchEntries[0])
                        0 -> log.warn("could not retrieve user by uid $uid").let { null }
                        else -> throw IllegalStateException("found multiple entries for uid $uid")
                    }
                }
            } catch (e: Exception) {
                log.error("Could not retrieve user from user with uid $uid", e)
                null
            }

    @Instrumentation.Timer("getGroupMembers")
    fun getGroupMembers(connection: LDAPConnection, group: Group): SortedSet<UserInfo> {
        return group.memberIds.map { cachedLdapService.getGroupMember(connection, it) }.toSortedSet()
    }

    @Instrumentation.Timer("findUserBySearchTerm")
    fun findUserBySearchTerm(connection: LDAPConnection, searchTerm: String): Set<UserInfo> {
        return try {
            val searchResult = connection.search(ldapConfiguration.config.baseDN,
                    SearchScope.SUB, LdapQueries.searchUser("*$searchTerm*"), "uid")

            searchResult.searchEntries.map {
                cachedLdapService.getGroupMember(connection, it.getAttributeValue("uid"))
            }.toSortedSet()
        } catch (e: Exception) {
            log.error("Could not find user by searchTermn $searchTerm", e)
            emptySet()
        }
    }

    @Instrumentation.Timer("getGroups")
    @Cacheable("groups", key="'groups'")
    fun getGroups(connection: LDAPConnection) =
            try {
                connection.search(ldapConfiguration.config.groupDN, SearchScope.SUB, LdapQueries.listAllGroups).searchEntries.mapNotNull {
                    groupMapping.map(it)
                }.toList().sorted()
            } catch (e: Exception) {
                throw RuntimeException("Could not retrieve groups from ldap", e)
            }

    @Instrumentation.Timer("getGroupsByUser")
    fun getGroupsByUser(connection: LDAPConnection, uid: String, userDN: String) =
            try {
                with(connection.search(ldapConfiguration.config.groupDN, SearchScope.SUB, LdapQueries.findGroupsByUser(uid, userDN))) {
                    searchEntries.mapNotNull {
                        groupMapping.map(it)
                    }.toSortedSet()
                }
            } catch (e: Exception) {
                log.error("Could not retrieve groups by user $uid", e)
                emptySet<Group>()
            }

    private fun extractUidFromDN(uidOrDN: String) = if (uidOrDN.startsWith("uid=")) uidOrDN.substring(4, uidOrDN.indexOf(',')) else uidOrDN

    @Instrumentation.Timer("getUsersByGroup")
    fun getUsersByGroup(connection: LDAPConnection, group: Group) =
            try {
                group.memberIds.mapNotNull { uidOrDN ->
                    cachedLdapService.getUserByUid(connection, extractUidFromDN(uidOrDN))
                }.toSortedSet()
            } catch (e: Exception) {
                log.error("Could not retrieve users by group " + group.cn, e)
                emptySet<User>()
            }

    private fun updateGroupMember(connection: LDAPConnection, user: User, group: Group, modificationType: ModificationType) =
            try {
                connection.modify(group.dn, mapOf(
                        group.groupType.memberAttritube to if (group.groupType === Posix) user.uid else user.dn
                ).toModification(modificationType))
                getGroupByCN(connection, group.cn)
            } catch (le: LDAPException) {
                throw RuntimeException("Could not add user ${user.uid} to group ${group.cn}", le)
            }

    @CacheEvict("groups", key = "#group.cn")
    fun addUserToGroup(connection: LDAPConnection, user: User, group: Group) = updateGroupMember(connection, user, group, ADD)

    @CacheEvict("groups", key = "#group.cn")
    fun removeUserFromGroup(connection: LDAPConnection, user: User, group: Group) = updateGroupMember(connection, user, group, DELETE)

    @Throws(BusinessException::class)
    fun resetPassword(connection: LDAPConnection, user: User) = changePassword(connection, user, randomAlphanumeric(32, 33))

    @Throws(BusinessException::class)
    fun changePassword(connection: LDAPConnection, currentUser: LdapUserDetails, password: String) =
            with(getUserByUid(connection, currentUser.username)!!) {
                changePassword(connection, this, password)?.let {
                    currentUser.setPassword(it)
                }
            }

    @Throws(BusinessException::class)
    private fun changePassword(connection: LDAPConnection, user: User, newPassword: String): String? {
        val changes = mapOf(
                "userPassword" to if (asList(*environment.activeProfiles).contains("development")) newPassword.trim() else Encrypt.salt(newPassword),
                "sambaNTPassword" to Encrypt.samba(newPassword),
                "sambaPwdLastSet" to (System.currentTimeMillis() / 1000L).toString()
        ).toModification()

        try {
            val result = connection.modify(getUserByUid(connection, user.uid)!!.dn, changes)
            if (result.resultCode !== ResultCode.SUCCESS) {
                log.warn("Could not properly change password for user ${user.uid}. Reason: ${result.diagnosticMessage} Status: ${result.resultCode}")
            }
        } catch (le: LDAPException) {
            log.error("Could not change password for user ${user.uid}")
            return null
        }
        return newPassword
    }

    @CacheEvict(value = ["groupMembers", "users"], key = "#uid")
    fun changeUserState(connection: LDAPConnection, uid: String, state: User.State) =
            update(connection, getUserByUid(connection, uid)!!.copy(
                    szzStatus = state,
                    szzMailStatus = state
            ))

    @Throws(BusinessException::class)
    private fun generateEmployeeID(connection: LDAPConnection): String {
        for (i in 1..20) {
            with(UUID.randomUUID().toString()) {
                if (!isUserAttributeAlreadyUsed(connection, "employeeNumber", this)) return this
            }
        }
        throw BusinessException("Can't generate unique employee number after 20 retries.", "user.employeeNumber.cantFindUnique")
    }

    @Throws(BusinessException::class)
    @CacheEvict(value = ["groupMembers", "users"], key = "#user.uid")
    fun insert(connection: LDAPConnection, user: User): User? {
        try {
            val nextUserID = getNextUserID(connection)
            val uidSuggestion = getUidSuggestion(connection, user.uid, user.givenName, user.sn)
            val userWithDefaults = user.copy(
                    dn = ldapConfiguration.getUserBind(uidSuggestion, user.companyKey),
                    uid = uidSuggestion,
                    uidNumber = nextUserID,
                    mail = if (user.mail.isNotBlank()) user.mail else "${mailify(user.givenName)}.${mailify(user.sn)}@${domainConfiguration.mailDomain(user.description)}",
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
                is String -> if(it.isNotBlank()) Modification(modificationType, entry.key, it)
                                else Modification(modificationType, entry.key)
                is Number -> Modification(modificationType, entry.key, it.toString())
                is Map<*,*> -> Modification(modificationType, entry.key, it.map {entry -> "${entry.key}=${entry.value}"}.joinToString(separator = ","))
                else -> throw UnsupportedOperationException()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun User.toMap() = OBJECT_MAPPER.convertValue(this, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    fun User.toAttributes() = toMap().mapNotNull {
        // TODO cleanup types (generics)
        val k = it.key as String
        val v = it.value
        when (v) {
            is Number -> Attribute(k, v.toString())
            is String -> if(v.isNotBlank()) Attribute(k, v) else null
            is Collection<*> -> Attribute(k, v as Collection<String>)
            is Map<*, *> ->  Attribute(k, v.map {entry -> "${entry.key}=${entry.value}"}.joinToString(","))
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
    @CacheEvict(value = ["groupMembers", "users"], key = "#user.uid")
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

            val changedEntries = with(pUser.toMap()) {
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

            if (isChanged(user.description, pUser.description) || isChanged(user.szzStatus, pUser.szzStatus)) {
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
        val defaultGroups = ldapConfiguration.permissions.defaultGroups[user.description] ?: return

        try {
            ldapConfiguration.createConnection().use { connection ->
                connection.bind(managementConfiguration.user.bindDN,
                        managementConfiguration.user.password)

                defaultGroups.forEach { groupCn ->
                    getGroupByCN(connection, groupCn)?.let {
                        cachedLdapService.addUserToGroup(connection, user, it)
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
                        .mapNotNull { cachedLdapService.removeUserFromGroup(connection, user, it) }
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
                        LdapQueries.findUserByUidNumber(uidNumber.toString()))
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
                    LdapQueries.checkUniqAttribute(attribute, value), attribute)
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

    @Instrumentation.Timer("getAdminGroup")
    fun getAdminGroup(connection: LDAPConnection, group: Group) =
            when (group.groupClassification) {
                ADMIN -> group
                else -> with(cachedLdapService.getGroupByCN(connection, ldapConfiguration.groupPrefixes.adminCnFor(group))) {
                    when (groupClassification) {
                        ADMIN -> this
                        else -> cachedLdapService.getGroupByCN(connection, ldapConfiguration.permissions.ldapAdminGroup)
                    }
                }
            }

    fun getGroupAdmins(connection: LDAPConnection, group: Group) = getUsersByGroup(connection, getAdminGroup(connection, group))

    companion object {
        private val log = LoggerFactory.getLogger(LdapService::class.java)
        val OBJECT_MAPPER = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}
