package com.sinnerschrader.s2b.accounttool.logic.component.authorization;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)   // 1
@SpringBootTest
@ActiveProfiles({"test"})
public class AuthorizationTests
{

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	private LdapUserDetails admin;

	private LdapUserDetails userAdmin;

	private LdapUserDetails user;

	@Before
	public void initialize()
	{
		List<GrantedAuthority> adminAuthorities = new ArrayList<>();
		for (String group : ldapConfiguration.getAdministrationGroups())
		{
			adminAuthorities.add(new SimpleGrantedAuthority(group));
		}
		admin = new LdapUserDetails("uid=tesadm,ou=users,ou=e1c1,dc=example,dc=org",
			"tesadm", "Test Admin", "testuser", "e1c1", adminAuthorities, false, true);

		List<GrantedAuthority> userAdminAuthorities = new ArrayList<>();
		for (String group : ldapConfiguration.getUserAdministrationGroups())
		{
			userAdminAuthorities.add(new SimpleGrantedAuthority(group));
		}
		userAdmin = new LdapUserDetails("uid=tesuse,ou=users,ou=e1c1,dc=example,dc=org",
			"tesuse", "Tes Useradmin", "testuser", "e1c1", userAdminAuthorities, false, true);

		List<GrantedAuthority> userAuthorities = new ArrayList<>();
		for (String group : Arrays.asList("s2a-tes", "s2f-tes", "s2f-set", "s2i-set", "s2f-err", "s2-users", "s2-vpn"))
		{
			userAuthorities.add(new SimpleGrantedAuthority(group));
		}
		user = new LdapUserDetails("uid=tester,ou=users,ou=e1c1,dc=example,dc=org",
			"tester", "Tes Ter", "testuser", "e1c1", userAuthorities, false, true);

	}

	@Test
	public void testPermissions()
	{
		authorizationService.ensureUserAdministration(admin);
		authorizationService.ensureGroupAdministration(admin, "s2-users");

		Assert.assertTrue(authorizationService.isAdmin(admin));
		Assert.assertFalse(authorizationService.isAdmin(userAdmin));
		Assert.assertFalse(authorizationService.isAdmin(user));

		Assert.assertFalse(authorizationService.isUserAdministration(admin));
		Assert.assertTrue(authorizationService.isUserAdministration(userAdmin));
		Assert.assertFalse(authorizationService.isUserAdministration(user));

		Assert.assertTrue(authorizationService.isGroupAdmin(user, "s2a-tes"));
		Assert.assertTrue(authorizationService.isGroupAdmin(user, "s2f-tes"));
		Assert.assertTrue(authorizationService.isGroupAdmin(user, "s2i-set"));
		Assert.assertFalse(authorizationService.isGroupAdmin(user, "s2f-set"));
		Assert.assertFalse(authorizationService.isGroupAdmin(user, "s2f-err"));
	}

}
