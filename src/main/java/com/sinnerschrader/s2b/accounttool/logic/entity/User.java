package com.sinnerschrader.s2b.accounttool.logic.entity;

import com.sinnerschrader.s2b.accounttool.logic.ReflectionDiffBuilder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.Diffable;
import org.ocpsoft.prettytime.PrettyTime;

import java.beans.Transient;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;


/**
 * User Model from LDAP
 */
@Value
public class User implements Comparable<User>, Diffable<User> {

    public final static List<String> objectClasses = Collections.unmodifiableList(Arrays.asList(
        "person",
        "organizationalPerson",
        "inetOrgPerson",
        "posixAccount",
        "sambaSamAccount",
        "szzUser"
    ));

    /**
     * Full DN of LDAP
     * Example: "dn: uid=firlas,ou=users,ou=e1c1,dc=exampe,dc=org"
     */
    String dn;

    /**
     * Username based on first- and lastname
     * Has to be 6 or 8 Characters long.
     */
    String uid;

    /**
     * Unique User ID for PosixAccounts.
     */
    Integer uidNumber;

    /**
     * Numeric Group ID, current always set to 100
     */
    Integer gidNumber;

    String displayName;

    /**
     * Full name firstname + lastname. All special chars have to be stripped.
     * Has to match regexp "[A-Za-z0-9 -]+"
     */
    String gecos;

    /**
     * Full name firstname + lastname
     */
    String cn;

    /**
     * GivenName / Firstname
     */
    String givenName;

    /**
     * Surname / Lastname
     */
    String sn;

    /**
     * Custom home directory, for personal fileshare.
     * Pattern: /export/home/{USERNAME}
     */
    String homeDirectory;

    /**
     * PosixAccount part, but always set with "/bin/false"
     */
    String loginShell;

    /**
     * Date of Birth (always in year 1972)
     */
    LocalDate birthDate;

    /**
     * Seperated into constant part an calculated part based on uidNumber.
     * S-1-5-21-1517199603-1739104038-1321870143-2552
     */
    String sambaSID;

    /**
     * Currently not really used, so it is a constant: 0000000000000000000000000000000000000000000000000000000000000000
     */
    String sambaPasswordHistory;

    /**
     * Currently not used, so it is a constant: [U          ]
     */
    String sambaAcctFlags;

    /**
     * The E-Mail Address: firstname.lastname@example.com
     */
    String mail;

    /**
     * General Status of this account.
     */
    State szzStatus;

    /**
     * Status if the e-mail is synced and useable.
     */
    State szzMailStatus;

    /**
     * Samba Timestamp ( System.currentTimeMillis() / 1000 )
     */
    Long sambaPwdLastSet;

    /**
     * Department or Team of this employee
     * Example: Technik, Client Services, HR, Team Java Robusta, etc.
     */
    String ou;

    /**
     * The Organization where the Employee belongs to.
     */
    String o;

    /**
     * Type of employment; should be employeeType of inetOrgPerson.
     * Example: Mitarbeiter, Freelancer, Student, Praktikant
     */
    String description;

    /**
     * The office number
     */
    String telephoneNumber;

    /**
     * The business mobile number
     */
    String mobile;

    /**
     * Unique Employee Number. Generated from UUID
     */
    String employeeNumber; // 6cb2d8bc-e4b6-460c-bd6f-0743b520da1a

    /**
     * Your title from the contract.
     */
    String title;

    /**
     * Location where the employee mainly work.
     * Example: Berlin, Hamburg, Frankfurt, Muenchen, Prag
     */
    String l;

    /**
     * Day of entry
     */
    LocalDate employeeEntryDate;

    /**
     * Day of exit (1-31)
     */
    LocalDate employeeExitDate;

    /**
     * The Public SSH Key of the User.
     */
    String szzPublicKey;

    /**
     * The Company where the User belongs to. (see: companies on yaml configuration)
     */
    private transient String companyKey;

    public User(String dn, String uid, Integer uidNumber, Integer gidNumber, String displayName, String gecos,
                String cn, String givenName, String sn, String homeDirectory, String loginShell, LocalDate birthDate,
                String sambaSID, String sambaPasswordHistory, String sambaAcctFlags, String mail, State szzStatus,
                State szzMailStatus, Long sambaPwdLastSet, LocalDate employeeEntryDate, LocalDate employeeExitDate, String ou,
                String description, String telephoneNumber, String mobile, String employeeNumber, String title, String l,
                String szzPublicKey, String o, String companyKey) {
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
        this.szzStatus = defaultIfNull(szzStatus, State.undefined);
        this.szzMailStatus = defaultIfNull(szzMailStatus, State.undefined);
        this.sambaPwdLastSet = sambaPwdLastSet;
        this.ou = ou;
        this.description = description;
        this.telephoneNumber = defaultString(telephoneNumber, "");
        this.mobile = defaultString(mobile, "");
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

    @Transient
    public Date getLastPasswordChange() {
        return new Date(sambaPwdLastSet * 1000);
    }

    @Transient
    public String getPrettyLastPasswordChange() {
        return new PrettyTime().format(getLastPasswordChange());
    }

    @Override
    public DiffResult diff(User user) {
        return new ReflectionDiffBuilder(this, user, SHORT_PREFIX_STYLE).build();
    }

    public enum State {
        active, inactive, undefined;

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
