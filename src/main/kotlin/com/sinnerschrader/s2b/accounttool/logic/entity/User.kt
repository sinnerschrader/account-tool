package com.sinnerschrader.s2b.accounttool.logic.entity

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.CompareToBuilder
import org.apache.commons.lang3.builder.DiffResult
import org.apache.commons.lang3.builder.Diffable
import org.apache.commons.lang3.builder.ReflectionDiffBuilder
import org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE
import org.ocpsoft.prettytime.PrettyTime
import java.text.Normalizer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class User(
        val dn: String = "", // Full DN of LDAP - Example: "dn: uid=firlas,ou=users,ou=e1c1,dc=exampe,dc=org"
        val uid: String, // Username based on first- and lastname - Has to be 6 or 8 Characters long.
        val uidNumber: Int? = null, // Unique User ID for PosixAccounts.
        val gidNumber: Int = 100, // Numeric Group ID, current always set to 100
        val givenName: String,
        val sn: String,
        val displayName: String = "$givenName $sn (COMPANY)", // Full name firstname + lastname
        val gecos: String = asciify("$givenName $sn"),
        val cn: String = "$givenName $sn",
        val homeDirectory: String = "", // Custom home directory, for personal fileshare.  Pattern: /export/home/{USERNAME}
        val loginShell: String = "/bin/false",
        val birthDate: LocalDate?, // Date of Birth (always in year 1972)
        val sambaSID: String? = "", // Seperated into constant part an calculated part based on uidNumber.  S-1-5-21-1517199603-1739104038-1321870143-2552
        val sambaPasswordHistory: String = "0000000000000000000000000000000000000000000000000000000000000000", // Currently not really used
        val sambaAcctFlags: String? = "", // Currently not used, so it is a constant: [U          ]
        val mail: String,
        val szzStatus: State = State.inactive,
        val szzMailStatus: State = State.inactive,
        val sambaPwdLastSet: Long = (System.currentTimeMillis() / 1000L),
        val employeeEntryDate: LocalDate?,
        val employeeExitDate: LocalDate?,
        val ou: String, // department / team
        val description: String, // Type of employment; should be employeeType of inetOrgPerson. - Example: Mitarbeiter, Freelancer, Student, Praktikant
        val telephoneNumber: String = "",
        val mobile: String = "",
        val employeeNumber: String, // 6cb2d8bc-e4b6-460c-bd6f-0743b520da1a Unique Employee Number. Generated from UUID
        val title: String = "",
        val l: String, // location
        val szzPublicKey: String = "",
        val o: String, // organisation
        val companyKey: String, // The Company where the User belongs to. (see: companies on yaml configuration)
        val modifiersName: String = "",
        val modifytimestamp: String = ""
) : Comparable<User>, Diffable<User> {

    companion object {
        val objectClasses = listOf(
            "person",
            "organizationalPerson",
            "inetOrgPerson",
            "posixAccount",
            "sambaSamAccount",
            "szzUser"
        )
    }


    fun getPrettyModifytimestamp() =
        try {
            val f = DateTimeFormatter.ofPattern("uuuuMMddHHmmss[,SSS][.SSS]X")
            val odt = OffsetDateTime.parse(modifytimestamp, f)
            PrettyTime().format(Date(odt.toInstant().toEpochMilli()))!!
        } catch (e: Exception) {
            modifytimestamp
        }

    fun getLastPasswordChange() = Date(sambaPwdLastSet * 1000)
    fun getPrettyLastPasswordChange() = PrettyTime().format(getLastPasswordChange())
    fun getPrettyLastModified() = if (modifytimestamp.isNotBlank()) "${getPrettyModifytimestamp()} by ${getPrettyModifiersName()}" else ""
    fun getPrettyModifiersName() = Regex("^uid=([^,]+)").find(modifiersName)?.groupValues?.get(1) ?: modifiersName

    override fun compareTo(other: User) =
        CompareToBuilder().append(sn, other.sn).append(givenName, other.givenName).append(uid, other.uid).build()

    override fun diff(other: User?): DiffResult = ReflectionDiffBuilder(this, other, SHORT_PREFIX_STYLE).build()

    enum class State {
        active, inactive;

        companion object {
            fun fromString(value: String) = if (active.name.equals(value, ignoreCase = true)) active else inactive
        }
    }
}


// TODO copied from ldapservice
private fun asciify(value: String): String {
    val searchList = arrayOf("ä", "Ä", "ü", "Ü", "ö", "Ö", "ß")
    val replacementList = arrayOf("ae", "Ae", "ue", "Ue", "oe", "Oe", "ss")
    return Normalizer.normalize(
            StringUtils.replaceEach(StringUtils.trimToEmpty(value), searchList, replacementList),
            Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
}
