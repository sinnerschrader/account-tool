package com.sinnerschrader.s2b.accounttool.config.ldap;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.net.SocketFactory;
import java.beans.Transient;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@ConfigurationProperties(prefix = "ldap")
public class LdapConfiguration implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(LdapConfiguration.class);

	private LdapBaseConfig config = new LdapBaseConfig();

	private LdapQueries queries = new LdapQueries();

	private LdapPermissions permissions = new LdapPermissions();

	private LdapGroupPrefixes groupPrefixes;

	private List<String> companies = null;

	private transient Map<String, String> companiesAsMap;

	public LdapConfiguration() {
		this.companiesAsMap = new ConcurrentSkipListMap<>();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		config.afterPropertiesSet();
		queries.afterPropertiesSet();
		permissions.afterPropertiesSet();

		final String rawDataSeparator = ":";
		final int keyPadding = 5;
		for (String rawCompany : companies) {
			String[] keyAndValue = StringUtils.split(rawCompany, rawDataSeparator);
			if (keyAndValue.length == 2) {
				companiesAsMap.put(keyAndValue[0], keyAndValue[1]);
				log.debug(
						"Loaded company: [{}] : {}",
						StringUtils.leftPad(keyAndValue[0], keyPadding),
						keyAndValue[1]);
			} else {
				throw new IllegalStateException("Could not parse company configuration");
			}
		}

		log.info("LDAP Configuration was initialized");
	}

	public LdapGroupPrefixes getGroupPrefixes() {
		return groupPrefixes;
	}

	public void setCompaniesAsMap(Map<String, String> companiesAsMap) {
		this.companiesAsMap = companiesAsMap;
	}

	public LdapBaseConfig getConfig() {
		return config;
	}

	public LdapPermissions getPermissions() {
		return permissions;
	}

	public LdapQueries getQueries() {
		return queries;
	}

	@Transient
	public String getHost() {
		return config.getHost();
	}

	@Transient
	public int getPort() {
		return config.getPort();
	}

	@Transient
	public String getBaseDN() {
		return config.getBaseDN();
	}

	@Transient
	public List<String> getDefaultGroups() {
		return permissions.getDefaultGroups();
	}

	@Transient
	public String getLdapQueryByName(String queryName, String... parameter) {
		String query = queries.getQuery(queryName);
		if (query != null && !query.isEmpty()) {
			return replacePlaceholders(query, parameter);
		}
		throw new IllegalStateException("Could not find a LDAP Query for name '" + queryName + "'");
	}

	private String replacePlaceholders(String query, String... args) {
		if (args == null || args.length < 1) {
			return query;
		}
		String result = query != null ? query : "";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			result = result.replace("{" + i + "}", arg);
		}
		return result;
	}

	@Transient
	public LDAPConnection createConnection() throws LDAPException, GeneralSecurityException {
		SocketFactory socketFactory = null;
		if (config.isSsl()) {
			SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			socketFactory = sslUtil.createSSLSocketFactory();
		}
		return new LDAPConnection(socketFactory, config.getHost(), config.getPort());
	}

	@Transient
	public String getUserBind(String uid, String companyKey) {
		String userDN = config.getUserDnByCompany(companyKey);
		if (userDN != null && !userDN.isEmpty()) {
			return replacePlaceholders(userDN, uid);
		}
		throw new IllegalArgumentException(
				"The provided company key '" + companyKey + "'is not allowed or known");
	}

	@Transient
	public List<String> getAdministrationGroups() {
		return permissions.getAdmins();
	}

	@Transient
	public List<String> getUserAdministrationGroups() {
		return permissions.getUserAdmins();
	}

	@Transient
	public String getGroupDN() {
		return config.getGroupDN();
	}

	public List<String> getCompanies() {
		return companies;
	}

	@Transient
	public Map<String, String> getCompaniesAsMap() {
		return companiesAsMap;
	}

	public void setConfig(LdapBaseConfig config) {
		this.config = config;
	}

	public void setQueries(LdapQueries queries) {
		this.queries = queries;
	}

	public void setPermissions(LdapPermissions permissions) {
		this.permissions = permissions;
	}

	public void setCompanies(List<String> companies) {
		this.companies = companies;
	}

	public void setGroupPrefixes(LdapGroupPrefixes groupPrefixes) {
		this.groupPrefixes = groupPrefixes;
	}
}
