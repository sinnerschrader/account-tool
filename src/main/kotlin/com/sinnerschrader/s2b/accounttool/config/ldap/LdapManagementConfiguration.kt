package com.sinnerschrader.s2b.accounttool.config.ldap

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


/**
 * Management Configuration for handling LDAP Business and cleanup tasks
 */
@Component
@ConfigurationProperties(prefix = "ldap-management")
class LdapManagementConfiguration {
    var user: ManagementUser = ManagementUser()
    var leavingUsersInCW: Int = 4
    var notifyReceipients = mutableListOf<String>()
    var jobs: JobsConfiguration = JobsConfiguration()
    var trackedGroups = mutableListOf<String>()

    open class ManagementUser {
        var bindDN = ""
        var password = ""
    }

    open class JobsConfiguration {
        var isActive = false
        var updateUnmaintained = JobConfiguration()
        var notifyAboutUnmaintained = JobConfiguration()
    }

    open class JobConfiguration {
        var isActive = false
        var cronExpr = ""
    }
}
