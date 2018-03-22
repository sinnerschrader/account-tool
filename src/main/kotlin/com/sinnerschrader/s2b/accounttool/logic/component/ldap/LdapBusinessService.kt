package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration
import com.sinnerschrader.s2b.accounttool.logic.DateTimeHelper
import com.sinnerschrader.s2b.accounttool.logic.component.mail.AccountExpirationMail
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService
import com.sinnerschrader.s2b.accounttool.logic.component.mail.UnmaintainedUsersMail
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.unboundid.ldap.sdk.LDAPException
import org.apache.commons.lang3.ObjectUtils.defaultIfNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.security.GeneralSecurityException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Collections.sort
import java.util.concurrent.TimeUnit


@Service
class LdapBusinessService : InitializingBean {

    private var userCache: Cache<String, List<User>>? = null

    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var managementConfiguration: LdapManagementConfiguration

    @Autowired
    private lateinit var mailService: MailService

    val unmaintainedMailUsers: List<User>
        get() = getUsersByCacheType(ACTIVE_MAIL_ON_INACTIVE_USER)

    val unmaintainedExternals: List<User>
        get() = getUsersByCacheType(EXITED_ACTIVE_USERS)

    val leavingUsers: List<User>
        get() = getUsersByCacheType(NEAR_FUTURE_EXITING)

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        userCache = CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build()
    }

    @Scheduled(cron = "\${ldap-management.jobs.updateUnmaintained.cronExpr}")
    protected fun updateUnmaintained() {
        if (!managementConfiguration.jobs.updateUnmaintained.isActive || !managementConfiguration.jobs.isActive) {
            return
        }

        log.debug("Updating the informations about unmaintained accounts")
        val startTime = LocalDateTime.now()
        val nextWeeks = managementConfiguration.leavingUsersInCW
        val exitedActiveUsers = LinkedList<User>()
        val futureExitingUser = LinkedList<User>()
        val activeMailAccounts = LinkedList<User>()
        try {
            ldapConfiguration.createConnection().use { connection ->
                connection.bind(managementConfiguration.user.bindDN,
                        managementConfiguration.user.password)

                val userCount = ldapService.getUserCount(connection)
                val blockSize = 250
                val today = LocalDate.now()
                val nearFuture = today.plusWeeks(nextWeeks.toLong())
                var i = 0
                while (i < userCount) {
                    val users = ldapService.getUsers(connection, i, blockSize)
                    if (CollectionUtils.isEmpty(users)) {
                        i = i + blockSize
                        continue
                    }

                    for (user in users) {
                        val exitDate = user.employeeExitDate
                        if (exitDate != null && user.szzStatus === User.State.active) {
                            if (exitDate.isBefore(today)) {
                                exitedActiveUsers.add(user)
                            } else if (exitDate.isBefore(nearFuture)) {
                                futureExitingUser.add(user)
                            }
                        }
                        if (user.szzStatus === User.State.inactive && user.szzMailStatus === User.State.active) {
                            activeMailAccounts.add(user)
                        }

                    }
                    i = i + blockSize
                }
                log.debug("Found {} user who are exited but have active accounts", exitedActiveUsers.size)
                log.debug("Found {} user who are leaving in the next {} weeks", futureExitingUser.size, nextWeeks)
                log.debug("Found {} active mail addresses on inactive accounts", activeMailAccounts.size)

                sort(exitedActiveUsers)
                sort(futureExitingUser)
                sort(activeMailAccounts)

                userCache!!.put(EXITED_ACTIVE_USERS, exitedActiveUsers)
                userCache!!.put(NEAR_FUTURE_EXITING, futureExitingUser)
                userCache!!.put(ACTIVE_MAIL_ON_INACTIVE_USER, activeMailAccounts)

                log.info("Updated the informations about unmaintained accounts in {}",
                        DateTimeHelper.getDurationString(startTime, LocalDateTime.now(), ChronoUnit.MILLIS))
            }
        } catch (e: LDAPException) {
            log.error("Could not update unmaintained users", e)
        } catch (e: GeneralSecurityException) {
            log.error("Could not update unmaintained users", e)
        }

    }

    @Scheduled(cron = "\${ldap-management.jobs.notifyAboutUnmaintained.cronExpr}")
    protected fun notifyAboutUnmaintained() {
        if (!managementConfiguration.jobs.notifyAboutUnmaintained.isActive || !managementConfiguration.jobs.isActive) {
            return
        }

        if (userCache!!.size() > 0) {
            val to = managementConfiguration.notifyReceipients.toTypedArray()
            mailService.sendMail(to, UnmaintainedUsersMail(
                    leavingUsers = getUsersByCacheType(NEAR_FUTURE_EXITING),
                    unmaintainedMailUsers = getUsersByCacheType(ACTIVE_MAIL_ON_INACTIVE_USER),
                    unmaintainedUsers = getUsersByCacheType(EXITED_ACTIVE_USERS),
                    publicDomain = mailService.publicDomain))
        }
    }

    @Scheduled(cron = "\${ldap-management.jobs.notifyAboutExpiration.cronExpr}")
    fun notifyAboutExpiration() {
        val threshold = LocalDate.now().plusWeeks(2)
        val expiringAccounts = leavingUsers.filter {
            threshold.isAfter(it.employeeExitDate!!)
        }
        expiringAccounts.forEach {
            mailService.sendMail(listOf(it), AccountExpirationMail(it))
        }
    }

    private fun getUsersByCacheType(type: String): List<User> {
        return defaultIfNull<List<User>>(userCache!!.getIfPresent(type), emptyList())
    }

    fun addDefaultGroups(user: User) {
        val defaultGroups = ldapConfiguration.permissions.defaultGroups
        if (defaultGroups.isEmpty()) {
            log.debug("No default groups defined, skipped adding user to default groups")
            return
        }
        try {
            ldapConfiguration.createConnection().use { connection ->
                connection.bind(managementConfiguration.user.bindDN,
                        managementConfiguration.user.password)

                for (groupCn in defaultGroups) {
                    val group = ldapService.getGroupByCN(connection, groupCn)
                    if (group != null) {
                        ldapService.addUserToGroup(connection, user, group)
                    }
                }
                log.debug("Added user to {} default groups", defaultGroups.size)
            }
        } catch (e: LDAPException) {
            log.error("Could not add user to default groups", e)
        } catch (e: GeneralSecurityException) {
            log.error("Could not add user to default groups", e)
        }

    }

    fun delDefaulGroups(user: User) {
        val defaultGroups = ldapConfiguration.permissions.defaultGroups
        if (defaultGroups.isEmpty()) {
            log.debug("No default groups defined, skipped removing user from default groups")
            return
        }
        try {
            ldapConfiguration.createConnection().use { connection ->
                connection.bind(managementConfiguration.user.bindDN,
                        managementConfiguration.user.password)

                for (groupCn in defaultGroups) {
                    val group = ldapService.getGroupByCN(connection, groupCn)
                    if (group != null) {
                        ldapService.removeUserFromGroup(connection, user, group)
                    }
                }
                log.debug("Removed user from {} default groups", defaultGroups.size)
            }
        } catch (e: LDAPException) {
            log.error("Could not remove user from default groups", e)
        } catch (e: GeneralSecurityException) {
            log.error("Could not remove user from default groups", e)
        }

    }

    companion object {
        private val log = LoggerFactory.getLogger(LdapBusinessService::class.java)
        private const val NEAR_FUTURE_EXITING = "leavingUsers"
        private const val EXITED_ACTIVE_USERS = "unmaintainedUsers"
        private const val ACTIVE_MAIL_ON_INACTIVE_USER = "unmaintainedMailUsers"
    }
}
