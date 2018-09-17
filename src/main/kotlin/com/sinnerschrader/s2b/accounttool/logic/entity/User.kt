package com.sinnerschrader.s2b.accounttool.logic.entity

import com.fasterxml.jackson.annotation.JsonIgnore
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

/**
 *
 * @param dn Full DN of LDAP - Example: "dn: uid=firlas,ou=users,ou=e1c1,dc=exampe,dc=org"
 * @param uid Username based on first- and lastname - Has to be 6 or 8 Characters long.
 * @param uidNumber Unique User ID for PosixAccounts.
 * @param gidNumber Numeric Group ID, current always set to 100
 * @param homeDirectory Custom home directory, for personal fileshare.  Pattern: /export/home/{USERNAME}
 * @param sambaSID Seperated into constant part an calculated part based on uidNumber.  S-1-5-21-1517199603-1739104038-1321870143-2552
 * @param sambaAcctFlags Currently not used, so it is a constant: [U          ]
 * @param sambaPasswordHistory Currently not really used
 * @param ou department / team
 * @param description Type of employment; should be employeeType of inetOrgPerson. - Example: Mitarbeiter, Freelancer, Student, Praktikant
 * @param employeeNumber 6cb2d8bc-e4b6-460c-bd6f-0743b520da1a Unique Employee Number. Generated from UUID
 * @param l location
 * @param o organisation
 * @param companyKey The Company where the User belongs to. (see: companies on yaml configuration)
 */
data class User(
        @JsonIgnore val dn: String = "",
        val uid: String,
        val uidNumber: Int? = null,
        val gidNumber: Int = 100,
        val givenName: String,
        val sn: String,
        val displayName: String = "$givenName $sn (COMPANY)",
        val gecos: String = asciify("$givenName $sn"),
        val cn: String = "$givenName $sn",
        val homeDirectory: String = "",
        val loginShell: String = "/bin/false",
        val szzBirthDay: Int = -1,
        val szzBirthMonth: Int = -1,
        val sambaSID: String? = "",
        val sambaPasswordHistory: String = "0000000000000000000000000000000000000000000000000000000000000000",
        val sambaAcctFlags: String? = "",
        val mail: String,
        val szzStatus: State = State.inactive,
        val szzMailStatus: State = State.inactive,
        @JsonIgnore val sambaPwdLastSet: Long = (System.currentTimeMillis() / 1000L),
        val szzEntryDate: LocalDate?,
        val szzExitDate: LocalDate?,
        val ou: String,
        val description: String,
        val telephoneNumber: String = "",
        val mobile: String = "",
        val employeeNumber: String,
        val title: String = "",
        val l: String,
        val szzPublicKey: String = "",
        val szzExternalAccounts: Map<String, String> = emptyMap(),
        val o: String,
        @JsonIgnore val companyKey: String,
        @JsonIgnore val modifiersName: String = "",
        @JsonIgnore val modifytimestamp: String = ""
) : Comparable<User>, Diffable<User> {
    val objectClass = listOf(
            "person",
            "organizationalPerson",
            "inetOrgPerson",
            "posixAccount",
            "sambaSamAccount",
            "szzUser"
    )

    override fun compareTo(other: User) =
            CompareToBuilder().append(sn, other.sn).append(givenName, other.givenName).append(uid, other.uid).build()

    override fun diff(other: User): DiffResult = ReflectionDiffBuilder(this, other, SHORT_PREFIX_STYLE).build()

    enum class State {
        active, inactive;

        companion object {
            fun fromString(value: String) = if (active.name.equals(value, ignoreCase = true)) active else inactive
        }
    }

    @JsonIgnore fun getPrettyModifytimestamp() =
            try {
                val f = DateTimeFormatter.ofPattern("uuuuMMddHHmmss[,SSS][.SSS]X")
                val odt = OffsetDateTime.parse(modifytimestamp, f)
                PrettyTime().format(Date(odt.toInstant().toEpochMilli()))!!
            } catch (e: Exception) {
                modifytimestamp
            }

    @JsonIgnore fun getLastPasswordChange() = Date(sambaPwdLastSet * 1000)
    @JsonIgnore fun getPrettyLastPasswordChange() = PrettyTime().format(getLastPasswordChange())
    @JsonIgnore fun getPrettyLastModified() = if (modifytimestamp.isNotBlank()) "${getPrettyModifytimestamp()} by ${getPrettyModifiersName()}" else ""
    @JsonIgnore fun getPrettyModifiersName() = Regex("^uid=([^,]+)").find(modifiersName)?.groupValues?.get(1) ?: modifiersName
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
