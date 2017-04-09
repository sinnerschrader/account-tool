package com.sinnerschrader.s2b.accounttool.config;

import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.spring4.extension.SpringExtension;
import com.sinnerschrader.s2b.accounttool.config.embedded.LDAPServer;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.Encrypter;
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.PasswordEncrypter;
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.PlainTextEncrypter;
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.SambaEncrypter;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapBusinessService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapBusinessServiceImpl;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapServiceImpl;
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.GroupMapping;
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.ModelMaping;
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.UserMapping;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.GlobalMessageInterceptor;
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.LdapConnectionInterceptor;
import com.sinnerschrader.s2b.accounttool.presentation.interceptor.RequestInterceptor;
import com.sinnerschrader.s2b.accounttool.support.pebble.AccountToolExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

import java.util.Arrays;
import java.util.Locale;

@Configuration
@EnableAutoConfiguration
public class ApplicationConfig extends WebMvcConfigurerAdapter {

	private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

	@Autowired
	private Environment environment;

	private LdapConfiguration ldapConfiguration;

	private LdapManagementConfiguration ldapManagementConfiguration;

	private LdapService ldapService;

	private LdapBusinessService ldapBusinessService;

	private LdapConnectionInterceptor ldapConnectionInterceptor;

	private GlobalMessageInterceptor globalMessageInterceptor;

	private RequestInterceptor requestInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(requestInterceptor());
		registry.addInterceptor(ldapConnectionInterceptor());
		registry.addInterceptor(globalMessageInterceptor());
	}

	@Bean
	public RequestInterceptor requestInterceptor() {
		if (requestInterceptor == null) {
			requestInterceptor = new RequestInterceptor(environment);
		}
		return requestInterceptor;
	}

	@Bean
	public LdapConnectionInterceptor ldapConnectionInterceptor() {
		if (ldapConnectionInterceptor == null) {
			ldapConnectionInterceptor = new LdapConnectionInterceptor(ldapConfiguration);
		}
		return ldapConnectionInterceptor;
	}

	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setDefaultLocale(Locale.ENGLISH);
		resolver.setSupportedLocales(Arrays.asList(Locale.ENGLISH, Locale.US));
		return resolver;
	}

	@Bean
	public GlobalMessageInterceptor globalMessageInterceptor() {
		if (globalMessageInterceptor == null) {
			globalMessageInterceptor = new GlobalMessageInterceptor();
		}
		return globalMessageInterceptor;
	}

	@Bean
	public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
		return new ResourceUrlEncodingFilter();
	}

	@Bean(name = "ldapConfiguration")
	public LdapConfiguration ldapConfiguration() {
		if (ldapConfiguration == null) {
			ldapConfiguration = new LdapConfiguration();
		}
		return ldapConfiguration;
	}

	@Bean(name = "ldapService")
	public LdapService ldapService() {
		if (ldapService == null) {
			ldapService = new LdapServiceImpl();
		}
		return ldapService;
	}

	@Bean(name = "ldapManagementConfiguration")
	public LdapManagementConfiguration ldapManagementConfiguration() {
		if (ldapManagementConfiguration == null) {
			ldapManagementConfiguration = new LdapManagementConfiguration();
		}
		return ldapManagementConfiguration;
	}

	@Bean(name = "ldapBusinessService")
	public LdapBusinessService ldapBusinessService() {
		if (ldapBusinessService == null) {
			ldapBusinessService = new LdapBusinessServiceImpl();
		}
		return ldapBusinessService;
	}

	@Bean(name = "userMapping")
	public ModelMaping<User> userMapping() {
		return new UserMapping();
	}

	@Bean(name = "groupMapping")
	public ModelMaping<Group> groupMapping() {
		return new GroupMapping();
	}

	@Bean
	public Extension springExtension() {
		return new SpringExtension();
	}

	@Bean
	public Extension accountToolExtension() {
		return new AccountToolExtension();
	}

	@Bean(name = "passwordEncrypter")
	public Encrypter passwordEncrypter() {
		if (Arrays.asList(environment.getActiveProfiles()).contains("development")) {
			log.warn("Loading Plaintext crypter - dont use this on production");
			return new PlainTextEncrypter();
		}
		return new PasswordEncrypter();
	}

	@Bean(name = "sambaEncrypter")
	public Encrypter sambaEncrypter() {
		return new SambaEncrypter();
	}

	@Bean(name = "ldapServer")
	@Profile({"development", "test"})
	public LDAPServer ldapServer() {
		return new LDAPServer();
	}
}
