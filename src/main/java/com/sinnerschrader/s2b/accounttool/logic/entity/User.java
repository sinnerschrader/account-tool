package com.sinnerschrader.s2b.accounttool.logic.entity;

import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.beans.Transient;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * User Model from LDAP
 */
public final class User implements Comparable<User> {

	public static final List<String> objectClasses =
			Collections.unmodifiableList(
					Arrays.asList(
							"person",
							"organizationalPerson",
							"inetOrgPerson",
							"posixAccount",
							"sambaSamAccount",
							"szzUser"));

	/**
	 * Full DN of LDAP Example: "dn: uid=firlas,ou=users,ou=e1c1,dc=exampe,dc=org"
	 */
	private String dn;

	/**
	 * Username based on first- and lastname Has to be 6 or 8 Characters long.
	 */
	private String uid;

	/**
	 * Unique User ID for PosixAccounts.
	 */
	private Integer uidNumber;

	/**
	 * Numeric Group ID, current always set to 100
	 */
	private Integer gidNumber;

	private String displayName;

	/**
	 * Full name firstname + lastname. All special chars have to be stripped. Has to match regexp
	 * "[A-Za-z0-9 -]+"
	 */
	private String gecos;

	/**
	 * Full name firstname + lastname
	 */
	private String cn;

	/**
	 * GivenName / Firstname
	 */
	private String givenName;

	/**
	 * Surname / Lastname
	 */
	private String sn;

	/**
	 * Custom home directory, for personal fileshare. Pattern: /export/home/{USERNAME}
	 */
	private String homeDirectory;

	/**
	 * PosixAccount part, but always set with "/bin/false"
	 */
	private String loginShell;

	/**
	 * Date of Birth (always in year 1972)
	 */
	private LocalDate birthDate;

	/**
	 * Seperated into constant part an calculated part based on uidNumber.
	 * S-1-5-21-1517199603-1739104038-1321870143-2552
	 */
	private String sambaSID;

	/**
	 * Currently not really used, so it is a constant:
	 * 0000000000000000000000000000000000000000000000000000000000000000
	 */
	private String sambaPasswordHistory;

	/**
	 * Currently not used, so it is a constant: [U ]
	 */
	private String sambaAcctFlags;

	/**
	 * The E-Mail Address: firstname.lastname@example.com
	 */
	private String mail;

	/**
	 * General Status of this account.
	 */
	private State szzStatus = State.undefined; // active

	/**
	 * Status if the e-mail is synced and useable.
	 */
	private State szzMailStatus = State.undefined;

	/**
	 * Samba Timestamp ( System.currentTimeMillis() / 1000 )
	 */
	private Long sambaPwdLastSet;

	/**
	 * Department or Team of this employee Example: Technik, Client Services, HR, Team Java Robusta,
	 * etc.
	 */
	private String ou;

	/**
	 * The Organization where the Employee belongs to.
	 */
	private String o;

	/**
	 * Type of employment; should be employeeType of inetOrgPerson. Example: Mitarbeiter, Freelancer,
	 * Student, Praktikant
	 */
	private String description;

	/**
	 * The office number
	 */
	private String telephoneNumber;

	/**
	 * The business mobile number
	 */
	private String mobile;

	/**
	 * Unique Employee Number. Generated from UUID
	 */
	private String employeeNumber; // 6cb2d8bc-e4b6-460c-bd6f-0743b520da1a

	/**
	 * Your title from the contract.
	 */
	private String title;

	/**
	 * Location where the employee mainly work. Example: Berlin, Hamburg, Frankfurt, Muenchen, Prag
	 */
	private String l;

	/**
	 * Day of entry
	 */
	private LocalDate employeeEntryDate;

	/**
	 * Day of exit (1-31)
	 */
	private LocalDate employeeExitDate;

	/**
	 * The Public SSH Key of the User.
	 */
	private String szzPublicKey;

	/**
	 * The Company where the User belongs to. (see: companies on yaml configuration)
	 */
	private transient String companyKey;

	public User(
			String dn,
			String uid,
			Integer uidNumber,
			Integer gidNumber,
			String displayName,
			String gecos,
			String cn,
			String givenName,
			String sn,
			String homeDirectory,
			String loginShell,
			LocalDate birthDate,
			String sambaSID,
			String sambaPasswordHistory,
			String sambaAcctFlags,
			String mail,
			State szzStatus,
			State szzMailStatus,
			Long sambaPwdLastSet,
			LocalDate employeeEntryDate,
			LocalDate employeeExitDate,
			String ou,
			String description,
			String telephoneNumber,
			String mobile,
			String employeeNumber,
			String title,
			String l,
			String szzPublicKey,
			String o,
			String companyKey) {
		this.dn = dn;
		this.uid = uid;
		this.uidNumber = uidNumber;
		this.gidNumber = gidNumber;
		this.displayName = displayName;
		this.gecos = gecos;
		this.cn = cn;
		this.givenName = givenName;
		this.sn = sn;
		this.homeDirectory = homeDirectory;
		this.loginShell = loginShell;
		this.birthDate = birthDate;
		this.sambaSID = sambaSID;
		this.sambaPasswordHistory = sambaPasswordHistory;
		this.sambaAcctFlags = sambaAcctFlags;
		this.mail = mail;
		this.szzStatus = szzStatus != null ? szzStatus : State.undefined;
		this.szzMailStatus = szzMailStatus != null ? szzMailStatus : State.undefined;
		this.sambaPwdLastSet = sambaPwdLastSet;
		this.ou = ou;
		this.description = description;
		this.telephoneNumber = StringUtils.defaultString(telephoneNumber, "");
		this.mobile = StringUtils.defaultString(mobile, "");
		this.employeeNumber = employeeNumber;
		this.title = title;
		this.l = l;
		this.o = o;
		this.employeeEntryDate = employeeEntryDate;
		this.employeeExitDate = employeeExitDate;
		this.szzPublicKey = szzPublicKey;
		this.companyKey = companyKey;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof User)) return false;

		User user = (User) o;

		if (uid != null ? !uid.equals(user.uid) : user.uid != null) return false;
		if (uidNumber != null ? !uidNumber.equals(user.uidNumber) : user.uidNumber != null)
			return false;
		return mail != null ? mail.equals(user.mail) : user.mail == null;
	}

	@Override
	public int hashCode() {
		int result = uid != null ? uid.hashCode() : 0;
		result = 31 * result + (uidNumber != null ? uidNumber.hashCode() : 0);
		result = 31 * result + (mail != null ? mail.hashCode() : 0);
		return result;
	}

	public String getDn() {
		return dn;
	}

	@Override
	public int compareTo(User o) {
		int res = StringUtils.compareIgnoreCase(getSn(), o.getSn());
		if (res == 0) {
			res = StringUtils.compareIgnoreCase(getGivenName(), o.getGivenName());
			if (res == 0) {
				res = StringUtils.compareIgnoreCase(getUid(), o.getUid());
			}
		}
		return res;
	}

	public LocalDate getEmployeeEntryDate() {
		return employeeEntryDate;
	}

	public LocalDate getEmployeeExitDate() {
		return employeeExitDate;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getCompanyKey() {
		return companyKey;
	}

	public String getUid() {
		return uid;
	}

	public Integer getUidNumber() {
		return uidNumber;
	}

	public Integer getGidNumber() {
		return gidNumber;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getGecos() {
		return gecos;
	}

	public String getCn() {
		return cn;
	}

	public String getGivenName() {
		return givenName;
	}

	public String getSn() {
		return sn;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public String getLoginShell() {
		return loginShell;
	}

	public String getSambaSID() {
		return sambaSID;
	}

	public String getSambaPasswordHistory() {
		return sambaPasswordHistory;
	}

	public String getSambaAcctFlags() {
		return sambaAcctFlags;
	}

	public String getMail() {
		return mail;
	}

	public State getSzzStatus() {
		return szzStatus;
	}

	public State getSzzMailStatus() {
		return szzMailStatus;
	}

	public Long getSambaPwdLastSet() {
		return sambaPwdLastSet;
	}

	@Transient
	public Date getLastPasswordChange() {
		return new Date(sambaPwdLastSet * 1000);
	}

	@Transient
	public String getPrettyLastPasswordChange() {
		return new PrettyTime().format(getLastPasswordChange());
	}

	public String getO() {
		return o;
	}

	public String getOu() {
		return ou;
	}

	public String getDescription() {
		return description;
	}

	public String getTelephoneNumber() {
		return telephoneNumber;
	}

	public String getMobile() {
		return mobile;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public String getTitle() {
		return title;
	}

	public String getL() {
		return l;
	}

	public String getSzzPublicKey() {
		return szzPublicKey;
	}

	public enum State {
		active,
		inactive,
		undefined;

		public static State fromString(String value) {
			if (active.name().equalsIgnoreCase(value)) {
				return active;
			}
			if (inactive.name().equalsIgnoreCase(value)) {
				return inactive;
			}
			return undefined;
		}
	}
}
