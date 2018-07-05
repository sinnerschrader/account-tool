package com.sinnerschrader.s2b.accounttool.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "domain")
class DomainConfiguration {
    var primary = ""
    var public = ""
    var subdomains = mutableMapOf<String,String>()

    fun mailDomain(type: String) = "${subdomains[type]?.plus(".") ?: ""}$primary"
}
