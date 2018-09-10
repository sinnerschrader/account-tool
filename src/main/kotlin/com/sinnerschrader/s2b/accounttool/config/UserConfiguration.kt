package com.sinnerschrader.s2b.accounttool.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "user")
class UserConfiguration {
    var smbIdPrefix = ""
    var sambaFlags = ""
    var homeDirPrefix = ""
    var externalAccounts = mutableMapOf<String, ExternalAccount>()

    data class ExternalAccount(var url: String = "")
}
