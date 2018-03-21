package com.sinnerschrader.s2b.accounttool.config

import com.mitchellbosecke.pebble.spring4.extension.SpringExtension
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.GlobalMessageInterceptor
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.LdapConnectionInterceptor
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.RequestInterceptor
import com.sinnerschrader.s2b.accounttool.presentation.messaging.GlobalMessageFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter
import java.util.*


@Configuration
@EnableAutoConfiguration
@EnableCaching
class ApplicationConfig : WebMvcConfigurerAdapter() {

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    @Autowired
    private lateinit var globalMessageFactory: GlobalMessageFactory

    override fun addInterceptors(registry: InterceptorRegistry) {
        with(registry) {
            addInterceptor(RequestInterceptor(environment))
            addInterceptor(LdapConnectionInterceptor(ldapConfiguration))
            addInterceptor(GlobalMessageInterceptor(globalMessageFactory))
        }
    }

    @Bean
    fun localeResolver() =
        with(AcceptHeaderLocaleResolver()) {
            defaultLocale = Locale.ENGLISH
            supportedLocales = listOf(Locale.ENGLISH, Locale.US)
            this
        }

    @Bean
    fun resourceUrlEncodingFilter() = ResourceUrlEncodingFilter()

    @Bean
    fun springExtension() = SpringExtension()
}
