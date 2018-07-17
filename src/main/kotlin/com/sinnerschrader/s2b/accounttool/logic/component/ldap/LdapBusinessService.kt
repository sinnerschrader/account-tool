package com.sinnerschrader.s2b.accounttool.logic.component.ldap

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration
import com.sinnerschrader.s2b.accounttool.logic.component.mail.AccountExpirationMail
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService
import com.sinnerschrader.s2b.accounttool.logic.component.mail.UnmaintainedUsersMail
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LdapBusinessService {
    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var managementConfiguration: LdapManagementConfiguration

    @Autowired
    private lateinit var mailService: MailService

    fun leavingUsers(): List<User> {
        val today = LocalDate.now()
        val nextWeeks = managementConfiguration.leavingUsersInCW
        val nearFuture = today.plusWeeks(nextWeeks.toLong())

        return ldapConfiguration.createConnection().use { connection ->
            connection.bind(managementConfiguration.user.bindDN, managementConfiguration.user.password)
            ldapService.getUsers(connection).filter {
                val exitDate = it.szzExitDate
                (exitDate != null && it.szzStatus == User.State.active &&
                    !exitDate.isBefore(today) && exitDate.isBefore(nearFuture))

            }
        }
    }

    fun unmaintainedExternals(): List<User> {
        val today = LocalDate.now()
        return ldapConfiguration.createConnection().use { connection ->
            connection.bind(managementConfiguration.user.bindDN, managementConfiguration.user.password)
            ldapService.getUsers(connection).filter {
                val exitDate = it.szzExitDate
                (it.szzStatus == User.State.active && exitDate?.isBefore(today) ?: false)
            }
        }
    }

    fun unmaintainedMailUsers(): List<User> {
        return ldapConfiguration.createConnection().use { connection ->
            connection.bind(managementConfiguration.user.bindDN, managementConfiguration.user.password)
            ldapService.getUsers(connection).filter {
                (it.szzStatus === User.State.inactive && it.szzMailStatus === User.State.active)
            }
        }
    }

    @Scheduled(cron = "\${ldap-management.jobs.notifyAboutUnmaintained.cronExpr}")
    protected fun notifyAboutUnmaintained() {
        with(managementConfiguration.jobs){
            if(!notifyAboutUnmaintained.isActive || !isActive) return
        }

        val leavingUsers = leavingUsers()
        val unmaintainedMailUsers = unmaintainedMailUsers()
        val unmaintainedUsers = unmaintainedExternals()

        if (leavingUsers.size + unmaintainedMailUsers.size + unmaintainedUsers.size > 0) {
            val to = managementConfiguration.notifyReceipients.toTypedArray()
            mailService.sendMail(to, UnmaintainedUsersMail(
                    leavingUsers = leavingUsers,
                    unmaintainedMailUsers = unmaintainedMailUsers,
                    unmaintainedUsers = unmaintainedUsers,
                    publicDomain = mailService.publicDomain))
        }
    }

    @Scheduled(cron = "\${ldap-management.jobs.notifyAboutExpiration.cronExpr}")
    fun notifyAboutExpiration() {
        val threshold = LocalDate.now().plusWeeks(2)
        val expiringAccounts = leavingUsers().filter {
            threshold.isAfter(it.szzExitDate!!)
        }
        expiringAccounts.forEach {
            mailService.sendMail(listOf(it), AccountExpirationMail(it))
        }
    }
}
