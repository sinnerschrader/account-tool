package com.sinnerschrader.s2b.accounttool.logic.health;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.unboundid.ldap.sdk.LDAPConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * JMX Health Check Indicator for LDAP Connection
 */
@Component
public class LdapHealthIndicator extends AbstractHealthIndicator {

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		builder.withDetail("location", ldapConfiguration.getHost() + ":" + ldapConfiguration.getPort());
		LDAPConnection connection = ldapConfiguration.createConnection();
		connection.close();
		builder.up();
	}
}
