package com.sinnerschrader.s2b.accounttool.config.embedded;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom embedded LDAP Server for development purpose. This embedded LDAP server will be started on
 * development profile automatically. For production profile this spring bean will be skipped.
 */
public class LDAPServer implements ApplicationContextAware, InitializingBean, Destroyable {

	private static final Logger log = LoggerFactory.getLogger(LDAPServer.class);

	@Autowired
	private LdapConfiguration ldapConfiguration;

	private transient InMemoryDirectoryServer directoryServer = null;

	@Value("${ldap.embedded:false}")
	private boolean enabled;

	private ApplicationContext applicationContext;

	private File workingDirectory;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!enabled) return;

		log.info("Starting embedded LDAP Server");
		final InetAddress host = InetAddress.getByName(ldapConfiguration.getHost());
		final int port = ldapConfiguration.getPort();
		final String embeddedLDAPServerURL = "ldap://" + host.getHostAddress() + ":" + port;
		final String[] baseDNs = new String[]{"cn=config", ldapConfiguration.getBaseDN()};
		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDNs);
		config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", host, port, null));

		final String[] schemaLdifs =
				new String[]{
						"classpath:ldap/schema/01-system.ldif",
						"classpath:ldap/schema/02-core.ldif",
						"classpath:ldap/schema/03-cosine.ldif",
						"classpath:ldap/schema/04-nis.ldif",
						"classpath:ldap/schema/05-inetOrgPerson.ldif",
						"classpath:ldap/schema/06-ppolicy.ldif",
						"classpath:ldap/schema/07-samba.ldif",
						"classpath:ldap/schema/08-szz.ldif"
				};
		List<File> schemaFiles = new ArrayList<>();
		for (String schemaLdif : schemaLdifs) {
			log.debug("Loading Schema from File: {}", schemaLdif);
			schemaFiles.add(applicationContext.getResource(schemaLdif).getFile());
		}
		Schema customSchema = Schema.getSchema(schemaFiles);
		if (log.isTraceEnabled()) {
			for (ObjectClassDefinition ocd : customSchema.getObjectClasses()) {
				log.trace("Loaded objectClass {}", ocd.getNameOrOID());
			}
			for (AttributeTypeDefinition atd : customSchema.getAttributeTypes()) {
				log.trace("Loaded attributeType {}", atd.getNameOrOID());
			}
		}
		config.setSchema(Schema.mergeSchemas(Schema.getDefaultStandardSchema(), customSchema));
		log.info("Schema was initialized, loading data next");

		directoryServer = new InMemoryDirectoryServer(config);
		final String[] dataLdifs =
				new String[]{
						"classpath:ldap/data/01-company-structure.ldif",
						"classpath:ldap/data/02-groups.ldif",
						"classpath:ldap/data/03-testuser.ldif"
				};

		Resource ldifResource;
		for (String ldif : dataLdifs) {
			ldifResource = applicationContext.getResource(ldif);
			log.debug("Loading Data from File: {}", ldifResource.getFilename());
			directoryServer.importFromLDIF(false, new LDIFReader(ldifResource.getInputStream()));
		}
		log.info("The data was loaded successfully");

		directoryServer.startListening();
		log.info("Started embedded LDAP Service on {}", embeddedLDAPServerURL);
	}

	@Override
	public void destroy() throws DestroyFailedException {
		if (directoryServer != null) {
			directoryServer.shutDown(true);
		}
		directoryServer = null;
	}

	@Override
	public boolean isDestroyed() {
		return directoryServer != null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
