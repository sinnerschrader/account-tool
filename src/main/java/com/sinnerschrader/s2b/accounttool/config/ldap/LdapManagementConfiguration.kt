package com.sinnerschrader.s2b.accounttool.config.ldap

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


/**
 * Management Configuration for handling LDAP Business and cleanup tasks
 */
@Component
@ConfigurationProperties(prefix = "ldap-management")
open class LdapManagementConfiguration {
    var user: ManagementUser? = null
    var leavingUsersInCW: Int = 4
    var notifyReceipients: List<String>? = null
    var jobs: JobsConfiguration? = null
    var trackedGroups: List<String>? = ArrayList()

    open class ManagementUser {
        var bindDN: String? = null
        var password: String? = null
    }

    open class JobsConfiguration {
        var isActive: Boolean = false
        var updateUnmaintained: JobConfiguration? = null
        var notifyAboutUnmaintained: JobConfiguration? = null
    }

    open class JobConfiguration {
        var isActive = false
        var cronExpr: String? = null
    }
}
