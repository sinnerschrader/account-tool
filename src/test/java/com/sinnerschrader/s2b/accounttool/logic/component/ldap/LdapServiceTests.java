package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException;
import com.unboundid.ldap.sdk.LDAPConnection;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"test"})
public class LdapServiceTests {

	private static final Logger log = LoggerFactory.getLogger(LdapServiceTests.class);

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Autowired
	private LdapService ldapService;

	@Value("${test.ldap.user}")
	private String ldapUser = "";

	@Value("${test.ldap.company}")
	private String ldapUserCompany = "";

	@Value("${test.ldap.password}")
	private String ldapUserPassword = "";

	private transient LdapUserDetails currentUser = null;

	private LDAPConnection create(boolean bind) throws Exception {
		LDAPConnection connection = ldapConfiguration.createConnection();
		if (bind) {
			String bindDN = ldapConfiguration.getUserBind(ldapUser, ldapUserCompany);
			connection.bind(bindDN, ldapUserPassword);
		}
		return connection;
	}

	@Before
	public void login() throws Exception {
		LDAPConnection connection = create(false);

		User user = ldapService.getUserByUid(connection, ldapUser);
		Assert.assertNotNull(user);

		List<Group> groups = ldapService.getGroupsByUser(connection, user.getUid(), user.getDn());
		Assert.assertNotNull(groups);
		Assert.assertFalse(groups.isEmpty());

		List<GrantedAuthority> grantedAuthorities =
				groups
						.stream()
						.map(group -> new SimpleGrantedAuthority(group.getCn()))
						.collect(Collectors.toList());

		this.currentUser =
				new LdapUserDetails(
						user.getDn(),
						user.getUid(),
						user.getDisplayName(),
						ldapUserPassword,
						ldapUserCompany,
						grantedAuthorities,
						false,
						true);

		connection.close();
	}

	@Test
	public void testReadOperations() throws Exception {
		LDAPConnection connection = create(true);

		final String uid = "dontexists";
		final String userDN = "uid=dontexists,ou=users,ou=comp,dc=example,dc=org";

		Assert.assertNull(ldapService.getUserByUid(connection, uid));
		List<Group> userGroups = ldapService.getGroupsByUser(connection, uid, userDN);
		Assert.assertNotNull(userGroups);
		Assert.assertTrue(userGroups.isEmpty());

		List<Group> allGroups = ldapService.getGroups(connection);
		Group group = ldapService.getGroupByCN(connection, "company-vpn");
		Group dontExist = ldapService.getGroupByCN(connection, "thisgroupwillneverexist-hopefully");

		Assert.assertNotNull(allGroups);
		Assert.assertFalse(allGroups.isEmpty());
		Assert.assertNotNull(group);
		Assert.assertTrue(allGroups.contains(group));
		Assert.assertNull(dontExist);

		connection.close();
	}

	@Test
	public void testGroupTypes() throws Exception {
		LDAPConnection connection = create(true);

		User userToAdd = ldapService.getUserByUid(connection, "musmax");
		Group posixGroup = ldapService.getGroupByCN(connection, "team-cus");
		Group groupOfNames = ldapService.getGroupByCN(connection, "groupOfNames");
		Group groupOfUNames = ldapService.getGroupByCN(connection, "groupOfUniqueNames");

		Assert.assertTrue(posixGroup.getGroupType() == Group.GroupType.Posix);
		Assert.assertTrue(groupOfNames.getGroupType() == Group.GroupType.GroupOfNames);
		Assert.assertTrue(groupOfUNames.getGroupType() == Group.GroupType.GroupOfUniqueNames);

		Assert.assertFalse(posixGroup.hasMember(userToAdd));
		Assert.assertFalse(groupOfNames.hasMember(userToAdd));
		Assert.assertFalse(groupOfUNames.hasMember(userToAdd));

		posixGroup = ldapService.addUserToGroup(connection, userToAdd, posixGroup);
		groupOfNames = ldapService.addUserToGroup(connection, userToAdd, groupOfNames);
		groupOfUNames = ldapService.addUserToGroup(connection, userToAdd, groupOfUNames);

		Assert.assertTrue(posixGroup.hasMember(userToAdd));
		Assert.assertTrue(groupOfNames.hasMember(userToAdd));
		Assert.assertTrue(groupOfUNames.hasMember(userToAdd));

		Assert.assertTrue(posixGroup.hasMember(userToAdd.getUid()));
		Assert.assertFalse(posixGroup.hasMember(userToAdd.getDn()));

		Assert.assertTrue(groupOfNames.hasMember(userToAdd.getDn()));
		Assert.assertFalse(groupOfNames.hasMember(userToAdd.getUid()));

		Assert.assertTrue(groupOfUNames.hasMember(userToAdd.getDn()));
		Assert.assertFalse(groupOfUNames.hasMember(userToAdd.getUid()));

		posixGroup = ldapService.removeUserFromGroup(connection, userToAdd, posixGroup);
		groupOfNames = ldapService.removeUserFromGroup(connection, userToAdd, groupOfNames);
		groupOfUNames = ldapService.removeUserFromGroup(connection, userToAdd, groupOfUNames);

		Assert.assertFalse(posixGroup.hasMember(userToAdd));
		Assert.assertFalse(groupOfNames.hasMember(userToAdd));
		Assert.assertFalse(groupOfUNames.hasMember(userToAdd));

		connection.close();
	}

	private User createUser(String firstName, String lastName) {
		return createUser(firstName, lastName, null, null, null);
	}

	private User createUser(
			String firstName, String lastName, String uid, String email, String employeeNumber) {
		return new User(
				null,
				uid,
				null,
				null,
				null,
				null,
				firstName + " " + lastName,
				firstName,
				lastName,
				null,
				null,
				LocalDate.of(1972, 7, 1),
				null,
				null,
				null,
				email,
				User.State.active,
				User.State.active,
				null,
				LocalDate.of(1990, 1, 1),
				LocalDate.of(2100, 12, 31),
				"Team Instinct",
				"Imaginary Employee",
				"",
				"",
				employeeNumber,
				"Not a real Person",
				"Hamburg",
				null,
				"Example - Company 2",
				"e1c2");
	}

	@Test
	public void testCreateUser() throws Exception {
		LDAPConnection connection = create(true);

		User newUser = createUser("Guy", "Inçögnítò");
		User pUser = ldapService.insert(connection, newUser);

		Assert.assertFalse(StringUtils.containsAny(pUser.getGecos(), 'ç', 'ö', 'í', 'ò'));

		Assert.assertEquals(pUser.getUid(), "guyinc");
		Assert.assertEquals(pUser.getGecos(), "Guy Incoegnito");
		Assert.assertEquals(pUser.getMail(), "guy.incoegnito@example.com");
		Assert.assertNotNull(pUser.getUidNumber());
		Assert.assertNotNull(pUser.getLoginShell());
		Assert.assertFalse(pUser.getLoginShell().isEmpty());
		Assert.assertNotNull(pUser.getHomeDirectory());
		Assert.assertFalse(pUser.getHomeDirectory().isEmpty());

		Assert.assertNotNull(pUser.getTelephoneNumber());
		Assert.assertNotNull(pUser.getMobile());
		Assert.assertTrue(pUser.getTelephoneNumber().isEmpty());
		Assert.assertTrue(pUser.getMobile().isEmpty());

		connection.close();
	}

	@Test
	public void testCreateUserUserNameExceeding() throws Exception {
		List<User> users = new LinkedList<>();
		for (int i = 1; i <= 10; i++) {
			users.add(createUser("Maximilian", "Mustermann", null, i + ".test@example.com", null));
		}
		LDAPConnection connection = create(true);
		int i = 0;
		try {
			for (User user : users) {
				ldapService.insert(connection, user);
				i++;
			}
		} catch (BusinessException be) {
			Assert.assertEquals(be.getCode(), "user.create.usernames.exceeded");
		}
		log.debug("Username autosuggest exeeded after {} tries", i);
		connection.close();
	}

	private String rnd() {
		return RandomStringUtils.randomAlphabetic(16);
	}

	@Test
	public void testCreateUserUniqueAttributes() throws Exception {
		LDAPConnection connection = create(true);

		final String emailCom = ".test@example.com";
		String firstName = rnd();
		String lastName = rnd();

		User newUser = createUser(firstName, lastName);
		User pUser = ldapService.insert(connection, newUser);

		try {
			ldapService.insert(
					connection, createUser(rnd(), rnd(), pUser.getUid(), rnd() + emailCom, rnd()));
			Assert.fail();
		} catch (BusinessException be) {
			Assert.assertEquals(be.getCode(), "user.create.username.alreadyUsed");
		}

		try {
			ldapService.insert(connection, createUser(rnd(), rnd(), rnd(), pUser.getMail(), rnd()));
			Assert.fail();
		} catch (BusinessException be) {
			Assert.assertEquals(be.getCode(), "user.mail.alreadyUsed");
		}

		try {
			ldapService.insert(
					connection, createUser(rnd(), rnd(), rnd(), rnd() + emailCom, pUser.getEmployeeNumber()));
			Assert.fail();
		} catch (BusinessException be) {
			Assert.assertEquals(be.getCode(), "user.employeeNumber.alreadyUsed");
		}

		connection.close();
	}

	@Test
	public void testREsetPasswordAndSetNewPassword() throws Exception {
		LDAPConnection connection = create(true);

		final User newUser = createUser(rnd(), rnd());
		final User pUser = ldapService.insert(connection, newUser);
		final String initialPassword = ldapService.resetPassword(connection, pUser);

		Assert.assertTrue(StringUtils.isNotBlank(initialPassword));

		final String currentPassword = currentUser.getPassword();
		final String newPassword = rnd();
		boolean success;

		Assert.assertNotEquals(currentPassword, newPassword);

		success = ldapService.changePassword(connection, currentUser, newPassword);

		Assert.assertTrue(success);

		connection.close();
	}
}
