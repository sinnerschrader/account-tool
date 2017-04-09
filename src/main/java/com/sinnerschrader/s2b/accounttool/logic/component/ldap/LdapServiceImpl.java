package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.component.encryption.Encrypter;
import com.sinnerschrader.s2b.accounttool.logic.component.mapping.ModelMaping;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LdapServiceImpl implements LdapService {

	private static final Logger log = LoggerFactory.getLogger(LdapServiceImpl.class);

	private final Cache<String, List<String>> listingsCache;

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Value("${domain.primary}")
	private String primaryDomain;

	@Value("${domain.secondary}")
	private String secondaryDomain;

	@Value("${user.smbIdPrefix}")
	private String smbIdPrefix;

	@Value("${user.sambaFlags}")
	private String sambaFlags;

	@Value("${user.homeDirPrefix}")
	private String homeDirPrefix;

	@Value("${user.loginShell}")
	private String loginShell;

	@Value("${user.appendCompanyOnDisplayName}")
	private boolean appendCompanyOnDisplayName = false;

	@Resource(name = "passwordEncrypter")
	private Encrypter passwordEncrypter;

	@Resource(name = "sambaEncrypter")
	private Encrypter sambaEncrypter;

	@Resource(name = "userMapping")
	private ModelMaping<User> userMapping;

	@Resource(name = "groupMapping")
	private ModelMaping<Group> groupMapping;

	private transient Integer lastUserNumber = null;

	public LdapServiceImpl() {
		this.listingsCache = CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build();
	}

	@Override
	public int getUserCount(LDAPConnection connection) {
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getBaseDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("listAllUsers"));
			return searchResult.getEntryCount();
		} catch (Exception e) {
			log.error("Could not fetch count of all users", e);
		}
		return 0;
	}

	@Override
	public List<User> getUsers(LDAPConnection connection, int firstResult, int maxResults) {
		try {
			SearchRequest request =
					new SearchRequest(
							ldapConfiguration.getBaseDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("listAllUsers"));
			SearchResult searchResult = connection.search(request);
			int count = searchResult.getEntryCount();
			int fr = Math.min(Math.max(0, firstResult), count);
			int mr = Math.min(fr + maxResults, count);
			return userMapping.map(searchResult.getSearchEntries().subList(fr, mr));
		} catch (Exception e) {
			log.error("Could not fetch all users", e);
		}
		return Collections.emptyList();
	}

	private List<String> getListingFromCacheOrLdap(
			LDAPConnection connection, String cacheKey, String attribute) {
		List<String> result = listingsCache.getIfPresent(cacheKey);
		if (result == null) {
			try {
				Set<String> tempResult = new HashSet<>();
				SearchResult searchResult =
						connection.search(
								ldapConfiguration.getBaseDN(),
								SearchScope.SUB,
								ldapConfiguration.getLdapQueryByName("listAllUsers"),
								attribute);

				String value;
				for (SearchResultEntry entry : searchResult.getSearchEntries()) {
					value = StringUtils.trimToEmpty(entry.getAttributeValue(attribute));
					if (StringUtils.isNotBlank(value)) {
						tempResult.add(value);
					}
				}
				if (tempResult.size() > 0) {
					log.debug("Found {} entries for attribue {}", tempResult.size(), attribute);
					result = new ArrayList<>(tempResult);
					Collections.sort(result);
					listingsCache.put(cacheKey, result);
				}
			} catch (LDAPException le) {
				log.warn("Could not fetch employee types");
			}
		}
		return result;
	}

	@Override
	public List<String> getEmployeeType(LDAPConnection connection) {
		return getListingFromCacheOrLdap(connection, "employeeTypes", "description");
	}

	@Override
	public List<String> getLocations(LDAPConnection connection) {
		return getListingFromCacheOrLdap(connection, "locations", "l");
	}

	@Override
	public List<String> getDepartments(LDAPConnection connection) {
		return getListingFromCacheOrLdap(connection, "departments", "ou");
	}

	@Override
	public User getUserByUid(LDAPConnection connection, String uid) {
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getBaseDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("findUserByUid", uid));
			if (searchResult.getEntryCount() > 1) {
				final String msg = "Found multiple entries for uid \"" + uid + "\"";
				log.warn(msg);
				throw new IllegalStateException(msg);
			}
			if (searchResult.getEntryCount() < 1) {
				log.trace("Could not retrieve user by uid {} ", uid);
				return null;
			}
			SearchResultEntry entry = searchResult.getSearchEntries().get(0);
			if (userMapping.isCompatible(entry)) {
				return userMapping.map(entry);
			}
		} catch (Exception e) {
			log.error("Could not retrieve user from user with uid " + uid, e);
		}
		return null;
	}

	@Override
	public Group getGroupByCN(LDAPConnection connection, String groupCn) {
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getGroupDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("findGroupByCn", groupCn));
			if (searchResult.getEntryCount() > 1) {
				final String msg = "Found multiple entries for group cn \"" + groupCn + "\"";
				log.warn(msg);
				throw new IllegalStateException(msg);
			}
			if (searchResult.getEntryCount() < 1) {
				log.warn("Could not retrieve group by cn {} ", groupCn);
				return null;
			}
			SearchResultEntry entry = searchResult.getSearchEntries().get(0);
			if (groupMapping.isCompatible(entry)) {
				return groupMapping.map(entry);
			}
		} catch (Exception e) {
			log.error("Could not retrieve group from ldap with cn " + groupCn, e);
		}
		return null;
	}

	@Override
	public List<User> findUserBySearchTerm(LDAPConnection connection, String searchTerm) {
		List<User> result = new LinkedList<>();
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getBaseDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("searchUser", "*" + searchTerm + "*"));
			result.addAll(userMapping.map(searchResult.getSearchEntries()));
			Collections.sort(result);
		} catch (Exception e) {
			log.error("Could not find user by searchTermn " + searchTerm, e);
		}
		return result;
	}

	@Override
	public List<Group> getGroups(LDAPConnection connection) {
		List<Group> result = new LinkedList<>();
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getGroupDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("listAllGroups"));
			result.addAll(groupMapping.map(searchResult.getSearchEntries()));
			Collections.sort(result);
		} catch (Exception e) {
			log.error("Could not retrieve groups from ldap ", e);
		}
		return result;
	}

	@Override
	public List<Group> getGroupsByUser(LDAPConnection connection, String uid, String userDN) {
		List<Group> result = new LinkedList<>();
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getGroupDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName("findGroupsByUser", uid, userDN));
			result.addAll(groupMapping.map(searchResult.getSearchEntries()));
			Collections.sort(result);
		} catch (Exception e) {
			log.error("Could not retrieve groups by user " + uid, e);
		}
		return result;
	}

	private String extractUidFromDN(String uidOrDN) {
		if (StringUtils.startsWith(uidOrDN, "uid=")) {
			return StringUtils.substring(uidOrDN, 4, StringUtils.indexOf(uidOrDN, ','));
		}
		return uidOrDN;
	}

	@Override
	public List<User> getUsersByGroup(LDAPConnection connection, Group group) {
		List<User> users = new ArrayList<>();
		try {
			group
					.getMemberIds()
					.forEach(
							uidOrDN -> {
								User user = getUserByUid(connection, extractUidFromDN(uidOrDN));
								if (user != null) {
									users.add(user);
								}
							});
			Collections.sort(users);
		} catch (Exception e) {
			log.error("Could not retrieve users by group " + group.getCn(), e);
		}
		return users;
	}

	@Override
	public Group addUserToGroup(LDAPConnection connection, User user, Group group) {
		Group ldapGroup = getGroupByCN(connection, group.getCn());
		if (ldapGroup == null) return null;
		if (ldapGroup.hasMember(user)) return ldapGroup;
		try {
			final Group.GroupType groupType = ldapGroup.getGroupType();
			final String memberValue = groupType == Group.GroupType.Posix ? user.getUid() : user.getDn();
			final String memberAttribute = groupType.getMemberAttritube();
			Modification modification =
					new Modification(ModificationType.ADD, memberAttribute, memberValue);
			connection.modify(ldapGroup.getDn(), modification);
		} catch (LDAPException le) {
			log.error("Could not add user {} to group {}", user.getUid(), group.getCn());
			if (log.isDebugEnabled()) {
				log.error("Could not add user", le);
			}
			return ldapGroup;
		}
		return getGroupByCN(connection, group.getCn());
	}

	@Override
	public Group removeUserFromGroup(LDAPConnection connection, User user, Group group) {
		Group ldapGroup = getGroupByCN(connection, group.getCn());
		if (ldapGroup == null) return null;
		if (!ldapGroup.hasMember(user)) return ldapGroup;
		try {
			final Group.GroupType groupType = ldapGroup.getGroupType();
			final String memberValue = groupType == Group.GroupType.Posix ? user.getUid() : user.getDn();
			final String memberAttribute = groupType.getMemberAttritube();
			Modification modification =
					new Modification(ModificationType.DELETE, memberAttribute, memberValue);
			connection.modify(ldapGroup.getDn(), modification);
		} catch (LDAPException le) {
			log.error("Could not add user {} to group {}", user.getUid(), group.getCn());
			if (log.isDebugEnabled()) {
				log.error("Could not add user", le);
			}
			return ldapGroup;
		}
		return getGroupByCN(connection, group.getCn());
	}

	@Override
	public String resetPassword(LDAPConnection connection, User user) throws BusinessException {
		return changePassword(connection, user, RandomStringUtils.randomAlphanumeric(32, 33));
	}

	@Override
	public boolean changePassword(
			LDAPConnection connection, LdapUserDetails currentUser, String password)
			throws BusinessException {
		User user = getUserByUid(connection, currentUser.getUsername());
		String newPassword = changePassword(connection, user, password);
		if (newPassword != null) {
			currentUser.setPassword(newPassword);
		}
		return true;
	}

	private String changePassword(LDAPConnection connection, User user, String newPassword)
			throws BusinessException {
		final String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
		User ldapUser = getUserByUid(connection, user.getUid());

		String[] ldapNameGecos = ldapUser.getGecos().toLowerCase().split("\\s+");

		if (StringUtils.containsIgnoreCase(newPassword, ldapUser.getUid())
				|| StringUtils.containsIgnoreCase(newPassword, ldapUser.getSn())
				|| StringUtils.containsIgnoreCase(newPassword, ldapUser.getGivenName())
				|| StringUtils.containsAny(newPassword.toLowerCase(), ldapNameGecos)) {
			throw new BusinessException(
					"Password can't contain user data.", "user.changePassword.failed");
		}

		List<Modification> changes = new ArrayList<>();
		changes.add(
				new Modification(
						ModificationType.REPLACE, "userPassword", passwordEncrypter.encrypt(newPassword)));
		changes.add(
				new Modification(
						ModificationType.REPLACE, "sambaNTPassword", sambaEncrypter.encrypt(newPassword)));
		changes.add(new Modification(ModificationType.REPLACE, "sambaPwdLastSet", timestamp));
		try {
			LDAPResult result = connection.modify(ldapUser.getDn(), changes);
			if (result.getResultCode() != ResultCode.SUCCESS) {
				log.warn(
						"Could not properly change password for user {}. Reason: {} Status: {}",
						ldapUser.getUid(),
						result.getDiagnosticMessage(),
						result.getResultCode());
			}
		} catch (LDAPException le) {
			log.error("Could not change password for user {} ", user.getUid());
			if (log.isDebugEnabled()) {
				log.error("Could change password for user", le);
			}
			return null;
		}
		return newPassword;
	}

	@Override
	public User activate(LDAPConnection connection, User user) {
		User ldapUser = getUserByUid(connection, user.getUid());
		if (ldapUser == null) return user;

		final String[] freelancerValues = {"Freelancer", "Feelancer"};
		List<Modification> changes = new ArrayList<>();

		changes.add(new Modification(ModificationType.REPLACE, "szzStatus", "active"));
		changes.add(new Modification(ModificationType.REPLACE, "szzMailStatus", "active"));
		if (StringUtils.equalsAny(ldapUser.getTitle(), freelancerValues)
				|| StringUtils.equalsAny(ldapUser.getDescription(), freelancerValues)) {
			LocalDate exitDate = LocalDate.now().plusWeeks(4).plusDays(1);
			changes.add(
					new Modification(
							ModificationType.REPLACE, "szzExitDay", String.valueOf(exitDate.getDayOfMonth())));
			changes.add(
					new Modification(
							ModificationType.REPLACE, "szzExitMonth", String.valueOf(exitDate.getMonthValue())));
			changes.add(
					new Modification(
							ModificationType.REPLACE, "szzExitYear", String.valueOf(exitDate.getYear())));
		}
		try {
			LDAPResult result = connection.modify(ldapUser.getDn(), changes);
			if (result.getResultCode() != ResultCode.SUCCESS) {
				log.warn(
						"Could not properly activate user {}. Reason: {} Status: {}",
						ldapUser.getUid(),
						result.getDiagnosticMessage(),
						result.getResultCode());
			}
		} catch (LDAPException le) {
			log.error("Could not activate user {}. Reason: {}", ldapUser.getUid(), le.getResultString());
			if (log.isDebugEnabled()) {
				log.error("Could not activate user", le);
			}
			return ldapUser;
		}
		return getUserByUid(connection, ldapUser.getUid());
	}

	@Override
	public User deactivate(LDAPConnection connection, User user) {
		User ldapUser = getUserByUid(connection, user.getUid());
		if (ldapUser == null) return user;

		List<Modification> changes = new ArrayList<>();
		changes.add(new Modification(ModificationType.REPLACE, "szzStatus", "inactive"));
		changes.add(new Modification(ModificationType.REPLACE, "szzMailStatus", "inactive"));
		try {
			LDAPResult result = connection.modify(ldapUser.getDn(), changes);
			if (result.getResultCode() != ResultCode.SUCCESS) {
				log.warn(
						"Could not properly activate user {}. Reason: {} Status: {}",
						ldapUser.getUid(),
						result.getDiagnosticMessage(),
						result.getResultCode());
			}
		} catch (LDAPException le) {
			log.error("Could not activate user {}. Reason: {}", ldapUser.getUid(), le.getResultString());
			if (log.isDebugEnabled()) {
				log.error("Could not activate user", le);
			}
			return ldapUser;
		}
		return getUserByUid(connection, ldapUser.getUid());
	}

	private String createMail(
			String firstName, String surname, String domain, boolean shortFirstname) {
		String fn = mailify(firstName);
		String sn = mailify(surname);
		return StringUtils.join(
				((shortFirstname) ? String.valueOf(fn.charAt(0)) : fn), ".", sn, "@", domain);
	}

	private String generateEmployeeID(LDAPConnection connection) throws BusinessException {
		int i = 0;
		String tmpEmployeeNumber = UUID.randomUUID().toString();
		while (isUserAttributeAlreadyUsed(connection, "employeeNumber", tmpEmployeeNumber)) {
			tmpEmployeeNumber = UUID.randomUUID().toString();
			i++;
			if (i > 20) {
				throw new BusinessException(
						"Can't generate unique employee number after 20 retries.",
						"user.employeeNumber.cantFindUnique");
			}
		}
		return tmpEmployeeNumber;
	}

	@Override
	public User insert(LDAPConnection connection, User user) throws BusinessException {
		try {
			String mailError = "alreadyUsed";
			String mail = user.getMail();
			if (StringUtils.isBlank(mail)) {
				mail = createMail(user.getGivenName(), user.getSn(), primaryDomain, false);
				mailError = "autofillFailed";
			}
			if (isUserAttributeAlreadyUsed(connection, "mail", mail)) {
				throw new BusinessException("E-Mail Address already used.", "user.mail." + mailError);
			}
			String tmpEmployeeNumber = user.getEmployeeNumber();
			if (StringUtils.isBlank(tmpEmployeeNumber)) {
				tmpEmployeeNumber = generateEmployeeID(connection);
			} else {
				if (isUserAttributeAlreadyUsed(connection, "employeeNumber", tmpEmployeeNumber)) {
					throw new BusinessException(
							"The entered employeenumber is already in use", "user.employeeNumber.alreadyUsed");
				}
			}

			final String username =
					getUidSuggestion(connection, user.getUid(), user.getGivenName(), user.getSn());
			final String dn = ldapConfiguration.getUserBind(username, user.getCompanyKey());
			final String fullName = user.getGivenName() + " " + user.getSn();
			final String displayName = fullName + " (" + user.getCompanyKey().toUpperCase() + ")";
			final Integer uidNumber = getNextUserID(connection);
			final String password = RandomStringUtils.randomAlphanumeric(16, 33);
			final Integer gidNumber = 100;
			final String homeDirectory = homeDirPrefix + username;
			final String employeeNumber = tmpEmployeeNumber;
			final Long sambaTimestamp = System.currentTimeMillis() / 1000L;
			final String sambaSID = smbIdPrefix + (uidNumber * 2 + 1000);
			final String sambaPWHistory =
					"0000000000000000000000000000000000000000000000000000000000000000";

			List<Attribute> attributes = new ArrayList<>();

			// Default Values and LDAP specific entries
			attributes.add(new Attribute("objectClass", User.objectClasses));
			attributes.add(new Attribute("employeeNumber", employeeNumber));
			attributes.add(new Attribute("uidNumber", uidNumber.toString()));
			attributes.add(new Attribute("gidNumber", gidNumber.toString()));
			attributes.add(new Attribute("loginShell", loginShell));
			attributes.add(new Attribute("homeDirectory", homeDirectory));
			attributes.add(new Attribute("sambaSID", sambaSID));
			attributes.add(new Attribute("sambaAcctFlags", sambaFlags));
			attributes.add(new Attribute("sambaPasswordHistory", sambaPWHistory));
			attributes.add(new Attribute("sambaPwdLastSet", sambaTimestamp.toString()));
			attributes.add(new Attribute("sambaNTPassword", sambaEncrypter.encrypt(password)));
			attributes.add(new Attribute("userPassword", passwordEncrypter.encrypt(password)));
			//attributes.add(new Attribute("szzPublicKey", ""));

			// Person informations
			attributes.add(new Attribute("uid", username));
			attributes.add(new Attribute("givenName", user.getGivenName()));
			attributes.add(new Attribute("sn", user.getSn()));
			attributes.add(new Attribute("cn", fullName));
			attributes.add(
					new Attribute("displayName", appendCompanyOnDisplayName ? displayName : fullName));
			attributes.add(new Attribute("gecos", asciify(fullName)));

			// Organisational Entries
			attributes.add(new Attribute("o", user.getO()));
			attributes.add(new Attribute("ou", user.getOu()));
			attributes.add(new Attribute("title", user.getTitle()));
			attributes.add(new Attribute("l", user.getL()));
			attributes.add(new Attribute("description", user.getDescription()));

			// Contact informations
			attributes.add(new Attribute("mail", mail));
			if (StringUtils.isNotBlank(user.getTelephoneNumber())) {
				attributes.add(new Attribute("telephoneNumber", user.getTelephoneNumber()));
			}
			if (StringUtils.isNotBlank(user.getMobile())) {
				attributes.add(new Attribute("mobile", user.getMobile()));
			}

			// Birthday with Day and Month
			if (user.getBirthDate() != null) {
				LocalDate birth = user.getBirthDate();
				attributes.add(new Attribute("szzBirthDay", String.valueOf(birth.getDayOfMonth())));
				attributes.add(new Attribute("szzBirthMonth", String.valueOf(birth.getMonthValue())));
			}

			// Entry Date
			LocalDate entry = user.getEmployeeEntryDate();
			if (entry == null) {
				throw new BusinessException("Entry could not be null", "user.entry.required");
			}
			attributes.add(new Attribute("szzEntryDay", String.valueOf(entry.getDayOfMonth())));
			attributes.add(new Attribute("szzEntryMonth", String.valueOf(entry.getMonthValue())));
			attributes.add(new Attribute("szzEntryYear", String.valueOf(entry.getYear())));

			// Exit Date
			LocalDate exit = user.getEmployeeExitDate();
			if (exit == null) {
				throw new BusinessException("Exit could not be null", "user.exit.required");
			}
			attributes.add(new Attribute("szzExitDay", String.valueOf(exit.getDayOfMonth())));
			attributes.add(new Attribute("szzExitMonth", String.valueOf(exit.getMonthValue())));
			attributes.add(new Attribute("szzExitYear", String.valueOf(exit.getYear())));

			// States
			attributes.add(new Attribute("szzStatus", user.getSzzStatus().name()));
			attributes.add(new Attribute("szzMailStatus", user.getSzzMailStatus().name()));

			LDAPResult result = connection.add(dn, attributes);
			if (result.getResultCode() != ResultCode.SUCCESS) {
				log.warn(
						"Could not create new user with dn '{}' username '{}' and uidNumber '{}'. Reason: {} Status: {}",
						dn,
						username,
						uidNumber,
						result.getDiagnosticMessage(),
						result.getResultCode());

				Object[] args =
						new Object[]{
								result.getResultString(),
								result.getResultCode().getName(),
								result.getResultCode().intValue()
						};
				throw new BusinessException("LDAP rejected creation of user", "user.create.failed", args);
			}
			return getUserByUid(connection, username);
		} catch (LDAPException le) {
			final String msg = "Could not create user";
			log.error(msg);
			if (log.isDebugEnabled()) {
				log.error(msg, le);
			}
			Object[] args =
					new Object[]{
							le.getResultString(), le.getResultCode().getName(), le.getResultCode().intValue()
					};
			throw new BusinessException(msg, "user.create.failed", args, le);
		}
	}

	private String asciify(String value) {
		final String[] searchList = {"ä", "Ä", "ü", "Ü", "ö", "Ö", "ß"};
		final String[] replacementList = {"ae", "Ae", "ue", "Ue", "oe", "Oe", "ss"};
		return Normalizer.normalize(
				StringUtils.replaceEach(StringUtils.trimToEmpty(value), searchList, replacementList),
				Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
	}

	private String mailify(String value) {
		String temp = StringUtils.trimToEmpty(value);
		temp = StringUtils.replaceAll(temp, "\\.", "");
		temp = asciify(temp);
		temp = temp.toLowerCase();
		return StringUtils.replaceAll(temp, "[^a-z0-9]", "-");
	}

	/**
	 * This method generated some suggestions for usernames. This suggestions can also handle short
	 * names with 1 or 2 characters. Example: Viktor Gruber Result into following variables:
	 * fnBeginPart = vik snBeginPart = gru fnEndPart = tor snEndPart = ber
	 * <p>
	 * <p>Suggestions: vik + gru gru + vik vik + ber ber + vik tor + gru gru + tor tor + ber ber + tor
	 *
	 * @param firstname - firstnam of a person
	 * @param surname   - lastname of a person
	 * @return a set of suggestions for usernames
	 */
	private Set<String> createUidSuggestions(String firstname, String surname)
			throws BusinessException {
		if (StringUtils.isBlank(firstname) || StringUtils.isBlank(surname)) {
			throw new IllegalArgumentException("First- and Surname are now allowed to be null or empty");
		}
		if (firstname.length() < 3 || surname.length() <= 3) {
			throw new BusinessException(
					"Firstname and Lastname have to be at minimum 3 characters long",
					"user.create.usernames.dontmatch");
		}
		String fn = mailify(firstname);
		String sn = mailify(surname);

		Set<String> res = new LinkedHashSet<>();
		String name = StringUtils.substring(fn + sn, 0, 3);
		name = name + sn;
		name = StringUtils.substring(name, 0, 6);

		String fnBeginPart = StringUtils.substring(name, 0, 3);
		String snBeginPart = StringUtils.substring(name, 3);

		int pos = Math.max(Math.min(3, fn.length() - 3), 0);
		name = StringUtils.substring(fn + sn, pos, pos + 3);
		name = name + StringUtils.reverse(StringUtils.substring(StringUtils.reverse(sn), 0, 3));

		String fnEndPart = StringUtils.substring(name, 0, 3);
		String snEndPart = StringUtils.substring(name, 3);

		res.add(fnBeginPart + snBeginPart);
		res.add(snBeginPart + fnBeginPart);

		res.add(fnBeginPart + snEndPart);
		res.add(snEndPart + fnBeginPart);

		res.add(fnEndPart + snBeginPart);
		res.add(snBeginPart + fnEndPart);

		res.add(fnEndPart + snEndPart);
		res.add(snEndPart + fnEndPart);

		return res;
	}

	private String getUidSuggestion(
			LDAPConnection connection, String username, String firstName, String lastName)
			throws BusinessException {
		if (StringUtils.isNotBlank(username)) {
			if (getUserByUid(connection, username) != null) {
				throw new BusinessException(
						"Entered username is already used.", "user.create.username.alreadyUsed");
			}
			return username;
		}
		Set<String> uidSuggestions = createUidSuggestions(firstName, lastName);
		for (String uidSuggestion : uidSuggestions) {
			if (getUserByUid(connection, uidSuggestion) == null) {
				return uidSuggestion;
			}
		}
		throw new BusinessException(
				"Could not create username, all suggestions are already used",
				"user.create.usernames.exceeded");
	}

	@Override
	public User update(LDAPConnection connection, User user) throws BusinessException {
		User ldapUser = getUserByUid(connection, user.getUid());
		if (ldapUser == null) {
			throw new BusinessException(
					"The modification was called for a non existing user", "user.notExists");
		}
		try {
			ModifyDNRequest modifyDNRequest = null;
			List<Modification> changes = new ArrayList<>();
			if (!StringUtils.equals(ldapUser.getCompanyKey(), user.getCompanyKey())) {
				final boolean delete = true;
				final String currentDN = ldapUser.getDn();
				final String newDN = ldapConfiguration.getUserBind(user.getUid(), user.getCompanyKey());
				final String newRDN = StringUtils.split(newDN, ",")[0];
				final String superiorDN = newDN.replace(newRDN + ",", StringUtils.EMPTY);

				log.warn("Move user to other company. From: {} To: {} + {}", currentDN, newRDN, superiorDN);
				modifyDNRequest = new ModifyDNRequest(currentDN, newRDN, delete, superiorDN);

				changes.add(new Modification(ModificationType.REPLACE, "o", user.getO()));
			}

			// Default Values and LDAP specific entries
			if (isChanged(user.getEmployeeNumber(), ldapUser.getEmployeeNumber(), true)) {
				String employeeNumber = user.getEmployeeNumber();
				if (StringUtils.isBlank(employeeNumber)) {
					employeeNumber = generateEmployeeID(connection);
				} else if (isUserAttributeAlreadyUsed(
						connection, "employeeNumber", user.getEmployeeNumber())) {
					throw new BusinessException(
							"Entered Employeenumber already used", "user.modify.employeeNumber.alreadyUsed");
				}
				changes.add(new Modification(ModificationType.REPLACE, "employeeNumber", employeeNumber));
			}
			if (isChanged(user.getSzzPublicKey(), ldapUser.getSzzPublicKey())) {
				changes.add(
						new Modification(ModificationType.REPLACE, "szzPublicKey", user.getSzzPublicKey()));
			}

			// Organisational Entries
			if (isChanged(user.getOu(), ldapUser.getOu())) {
				changes.add(new Modification(ModificationType.REPLACE, "ou", user.getOu()));
			}
			if (isChanged(user.getTitle(), ldapUser.getTitle())) {
				changes.add(new Modification(ModificationType.REPLACE, "title", user.getTitle()));
			}
			if (isChanged(user.getL(), ldapUser.getL())) {
				changes.add(new Modification(ModificationType.REPLACE, "l", user.getL()));
			}
			if (isChanged(user.getDescription(), ldapUser.getDescription())) {
				changes.add(
						new Modification(ModificationType.REPLACE, "description", user.getDescription()));
			}

			// Contact informations
			if (isChanged(user.getTelephoneNumber(), ldapUser.getTelephoneNumber(), true)) {
				if (StringUtils.isBlank(user.getTelephoneNumber())) {
					changes.add(new Modification(ModificationType.DELETE, "telephoneNumber"));
				} else {
					changes.add(
							new Modification(
									ModificationType.REPLACE, "telephoneNumber", user.getTelephoneNumber()));
				}
			}
			if (isChanged(user.getMobile(), ldapUser.getMobile(), true)) {
				if (StringUtils.isBlank(user.getMobile())) {
					changes.add(new Modification(ModificationType.DELETE, "mobile"));
				} else {
					changes.add(new Modification(ModificationType.REPLACE, "mobile", user.getMobile()));
				}
			}

			// Birthday with Day and Month
			LocalDate birth = user.getBirthDate();
			if (birth != null && isChanged(birth, ldapUser.getBirthDate())) {
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzBirthDay", String.valueOf(birth.getDayOfMonth())));
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzBirthMonth", String.valueOf(birth.getMonthValue())));
			} else if (birth == null && ldapUser.getBirthDate() != null) {
				changes.add(new Modification(ModificationType.DELETE, "szzBirthDay"));
				changes.add(new Modification(ModificationType.DELETE, "szzBirthMonth"));
			}

			// Entry Date
			LocalDate entry = user.getEmployeeEntryDate();
			if (entry != null && isChanged(entry, ldapUser.getEmployeeEntryDate())) {
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzEntryDay", String.valueOf(entry.getDayOfMonth())));
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzEntryMonth", String.valueOf(entry.getMonthValue())));
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzEntryYear", String.valueOf(entry.getYear())));
			}

			// Exit Date
			LocalDate exit = user.getEmployeeExitDate();
			if (exit != null && isChanged(exit, ldapUser.getEmployeeExitDate())) {
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzExitDay", String.valueOf(exit.getDayOfMonth())));
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzExitMonth", String.valueOf(exit.getMonthValue())));
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzExitYear", String.valueOf(exit.getYear())));
			}

			// States
			if (isChanged(user.getSzzStatus(), ldapUser.getSzzStatus())
					&& user.getSzzStatus() != User.State.undefined) {
				changes.add(
						new Modification(ModificationType.REPLACE, "szzStatus", user.getSzzStatus().name()));
			}
			if (isChanged(user.getSzzMailStatus(), ldapUser.getSzzMailStatus())
					&& user.getSzzMailStatus() != User.State.undefined) {
				changes.add(
						new Modification(
								ModificationType.REPLACE, "szzMailStatus", user.getSzzMailStatus().name()));
			}

			LDAPResult result = null;
			// save modifications
			if (!changes.isEmpty()) {
				result = connection.modify(ldapUser.getDn(), changes);
				if (result.getResultCode() != ResultCode.SUCCESS) {
					log.warn(
							"Could not modify user with dn '{}' username '{}'. Reason: {} Status: {}",
							ldapUser.getDn(),
							ldapUser.getUid(),
							result.getDiagnosticMessage(),
							result.getResultCode());

					Object[] args =
							new Object[]{
									result.getResultString(),
									result.getResultCode().getName(),
									result.getResultCode().intValue()
							};
					throw new BusinessException("LDAP rejected update of user", "user.modify.failed", args);
				}
			}
			// move user to other DN (Company)
			if (modifyDNRequest != null) {
				result = connection.modifyDN(modifyDNRequest);
				if (result.getResultCode() != ResultCode.SUCCESS) {
					log.warn(
							"Could move user to other Company '{}' username '{}'. Reason: {} Status: {}",
							ldapUser.getDn(),
							ldapUser.getUid(),
							result.getDiagnosticMessage(),
							result.getResultCode());

					Object[] args =
							new Object[]{
									result.getResultString(),
									result.getResultCode().getName(),
									result.getResultCode().intValue()
							};
					throw new BusinessException("LDAP rejected update of user", "user.modify.failed", args);
				}
			}
		} catch (LDAPException le) {
			final String msg = "Could not change user";
			log.error(msg);
			if (log.isDebugEnabled()) {
				log.error(msg, le);
			}
			Object[] args =
					new Object[]{
							le.getResultString(), le.getResultCode().getName(), le.getResultCode().intValue()
					};
			throw new BusinessException(msg, "user.modify.failed", args, le);
		}
		return getUserByUid(connection, user.getUid());
	}

	private Integer fetchMaxUserIDNumber(LDAPConnection connection) {
		final String queryName = "listAllUsers";
		final String attribute = "uidNumber";
		Integer result = 1000;
		try {
			SearchResult searchResult =
					connection.search(
							ldapConfiguration.getBaseDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName(queryName),
							attribute);

			Integer uidNumber;
			for (SearchResultEntry entry : searchResult.getSearchEntries()) {
				uidNumber = entry.getAttributeValueAsInteger(attribute);
				if (uidNumber > result) {
					result = uidNumber;
				}
			}
		} catch (LDAPException le) {
			log.warn("Could not fetch employee types");
		}
		return result;
	}

	private synchronized int getNextUserID(LDAPConnection connection) throws BusinessException {
		if (lastUserNumber == null) {
			lastUserNumber = fetchMaxUserIDNumber(connection);
		}
		final String queryName = "findUserByUidNumber";
		final int maxTriesForNextUidNumber = 1000;
		final int maxUserNumber = lastUserNumber + maxTriesForNextUidNumber;
		for (int uidNumber = lastUserNumber + 1; uidNumber < maxUserNumber; uidNumber++) {
			try {
				SearchResult searchResult =
						connection.search(
								ldapConfiguration.getBaseDN(),
								SearchScope.SUB,
								ldapConfiguration.getLdapQueryByName(queryName, String.valueOf(uidNumber)));
				if (searchResult.getEntryCount() == 0) {
					lastUserNumber = uidNumber;
					return uidNumber;
				}
			} catch (LDAPException le) {
				log.error(
						"Could not fetch next uidNumber for new user, Reason: {}, result: {}",
						le.getDiagnosticMessage(),
						le.getResultString());
				if (log.isDebugEnabled()) {
					log.error("Could not fetch next uidNumber for new user", le);
				}
			}
		}
		throw new BusinessException("Could not find a valid new uid Number.", "uidNumber.exceeded");
	}

	private boolean isChanged(Object newValue, Object originalValue) {
		return isChanged(newValue, originalValue, false);
	}

	private boolean isChanged(Object newValue, Object originalValue, boolean removeable) {
		return (removeable || (newValue != null && !newValue.equals("")))
				&& !newValue.equals(originalValue);
	}

	private boolean isUserAttributeAlreadyUsed(
			LDAPConnection connection, String attribute, String value) throws BusinessException {
		final String queryName = "checkUniqAttribute";
		try {
			SearchResult result =
					connection.search(
							ldapConfiguration.getBaseDN(),
							SearchScope.SUB,
							ldapConfiguration.getLdapQueryByName(queryName, attribute, value),
							attribute);
			if (result.getResultCode() == ResultCode.SUCCESS) {
				return result.getEntryCount() != 0;
			}
			throw new BusinessException(
					"Could not check attribute", "user.uniqAttributeCheck", new Object[]{attribute});
		} catch (LDAPException le) {
			throw new BusinessException(
					"Could not check attribute",
					"general.ldap.failed",
					new Object[]{le.getDiagnosticMessage()});
		}
	}
}
