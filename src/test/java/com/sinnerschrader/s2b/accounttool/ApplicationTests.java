package com.sinnerschrader.s2b.accounttool;

import com.sinnerschrader.s2b.accounttool.config.embedded.LDAPServer;
import com.sinnerschrader.s2b.accounttool.logic.component.authorization.AuthorizationService;
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"test"})
public class ApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void contextLoads() {
		Assert.isTrue(applicationContext.getEnvironment().acceptsProfiles("test"));
		Assert.notNull(applicationContext.getBean(AuthorizationService.class));
		Assert.notNull(applicationContext.getBean(LDAPServer.class));
		Assert.notNull(applicationContext.getBean(LdapService.class));

		Assert.notNull(applicationContext.getBean("ldapServer"));
		Assert.notNull(applicationContext.getBean("ldapConfiguration"));
		Assert.notNull(applicationContext.getBean("groupMapping"));
		Assert.notNull(applicationContext.getBean("userMapping"));
	}
}
