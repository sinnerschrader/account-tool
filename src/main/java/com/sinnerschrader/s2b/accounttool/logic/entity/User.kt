package com.sinnerschrader.s2b.accounttool.logic.entity

import org.apache.commons.lang3.builder.CompareToBuilder
import org.apache.commons.lang3.builder.DiffResult
import org.apache.commons.lang3.builder.Diffable
import org.apache.commons.lang3.builder.ReflectionDiffBuilder
import org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE
import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Arrays.asList
import java.util.Collections.unmodifiableList


data class User(
    /**
     * Full DN of LDAP
     * Example: "dn: uid=firlas,ou=users,ou=e1c1,dc=exampe,dc=org"
     */
    val dn: String? = "",

    /**
     * Username based on first- and lastname
     * Has to be 6 or 8 Characters long.
     */
    val uid: String,

    /**
     * Unique User ID for PosixAccounts.
     */
    val uidNumber: Int? = null,

    /**
     * Numeric Group ID, current always set to 100
     */
    val gidNumber: Int? = null,

    val displayName: String?,

    /**
     * Full name firstname + lastname. All special chars have to be stripped.
     * Has to match regexp "[A-Za-z0-9 -]+"
     */
    val gecos: String?,

    /**
     * Full name firstname + lastname
     */
    val cn: String,

    /**
     * GivenName / Firstname
     */
    val givenName: String,

    /**
     * Surname / Lastname
     */
    val sn: String,

    /**
     * Custom home directory, for personal fileshare.
     * Pattern: /export/home/{USERNAME}
     */
    val homeDirectory: String? = "",

    /**
     * PosixAccount part, but always set with "/bin/false"
     */
    val loginShell: String? = "",

    /**
     * Date of Birth (always in year 1972)
     */
    val birthDate: LocalDate?,

    /**
     * Seperated into constant part an calculated part based on uidNumber.
     * S-1-5-21-1517199603-1739104038-1321870143-2552
     */
    val sambaSID: String? = "",

    /**
     * Currently not really used, so it is a constant: 0000000000000000000000000000000000000000000000000000000000000000
     */
    val sambaPasswordHistory: String? = "",

    /**
     * Currently not used, so it is a constant: [U          ]
     */
    val sambaAcctFlags: String? = "",

    /**
     * The E-Mail Address: firstname.lastname@example.com
     */
    val mail: String,

    /**
     * General Status of this account.
     */
    val szzStatus: State = User.State.undefined,

    /**
     * Status if the e-mail is synced and useable.
     */
    val szzMailStatus: State = User.State.undefined,

    /**
     * Samba Timestamp ( System.currentTimeMillis() / 1000 )
     */
    val sambaPwdLastSet: Long,

    /**
     * Day of entry
     */
    val employeeEntryDate: LocalDate?,

    /**
     * Day of exit (1-31)
     */
    val employeeExitDate: LocalDate?,

    /**
     * Department or Team of this employee
     * Example: Technik, Client Services, HR, Team Java Robusta, etc.
     */
    val ou: String,

    /**
     * Type of employment; should be employeeType of inetOrgPerson.
     * Example: Mitarbeiter, Freelancer, Student, Praktikant
     */
    val description: String,

    /**
     * The office number
     */
    val telephoneNumber: String,

    /**
     * The business mobile number
     */
    val mobile: String,

    /**
     * Unique Employee Number. Generated from UUID
     */
    val employeeNumber: String, // 6cb2d8bc-e4b6-460c-bd6f-0743b520da1a


    /**
     * Your title from the contract.
     */
    val title: String,

    /**
     * Location where the employee mainly work.
     * Example: Berlin, Hamburg, Frankfurt, Muenchen, Prag
     */
    val l: String,

    /**
     * The Public SSH Key of the User.
     */
    val szzPublicKey: String? = "",

    /**
     * The Organization where the Employee belongs to.
     */
    val o: String,

    /**
     * The Company where the User belongs to. (see: companies on yaml configuration)
     */
    val companyKey: String,

    val modifiersName: String = "",

    val modifytimestamp: String = ""

) : Comparable<User>, Diffable<User> {

    companion object {
        val objectClasses = unmodifiableList(asList(
            "person",
            "organizationalPerson",
            "inetOrgPerson",
            "posixAccount",
            "sambaSamAccount",
            "szzUser"
        ))
    }

    fun getLastPasswordChange() = Date(sambaPwdLastSet * 1000)

    fun getPrettyModifytimestamp(): String {
        try {
            val f = DateTimeFormatter.ofPattern("uuuuMMddHHmmss[,SSS][.SSS]X")
            val odt = OffsetDateTime.parse(modifytimestamp, f)
            return PrettyTime().format(Date(odt.toInstant().toEpochMilli()))
        } catch (e: Exception) {
            return modifytimestamp
        }

    }

    fun getPrettyLastPasswordChange() = PrettyTime().format(getLastPasswordChange())

    fun getPrettyModifiersName() = Regex("^uid=([^,]+)").find(modifiersName)?.groupValues?.get(1) ?: modifiersName

    override fun compareTo(other: User) =
        CompareToBuilder().append(sn, other.sn).append(givenName, other.givenName).append(uid, other.uid).build()


    override fun diff(other: User?): DiffResult = ReflectionDiffBuilder(this, other, SHORT_PREFIX_STYLE).build()

    enum class State {
        active, inactive, undefined;


        companion object {

            fun fromString(value: String): State {
                if (active.name.equals(value, ignoreCase = true)) {
                    return active
                }
                if (inactive.name.equals(value, ignoreCase = true)) {
                    return inactive
                }
                return undefined
            }
        }
    }
}


