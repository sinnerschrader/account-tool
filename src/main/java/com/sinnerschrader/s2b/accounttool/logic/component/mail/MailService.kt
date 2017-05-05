package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.error.PebbleException
import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails
import com.sinnerschrader.s2b.accounttool.logic.entity.Group
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.util.*


@Service
class MailService {

    companion object {
        private val log = LoggerFactory.getLogger(MailService::class.java)

        enum class Change {ADD, REMOVE }
    }

    @Autowired
    lateinit var pebbleEngine: PebbleEngine

    @Autowired
    lateinit var javaMailSender: JavaMailSender

    @Value("\${domain.public}")
    lateinit var publicDomain: String

    @Value("\${spring.mail.from}")
    lateinit var from: String

    @Value("\${spring.mail.reply}")
    lateinit var reply: String

    @Value("\${spring.mail.logOnly}")
    var logOnly: Boolean = false

    fun sendMailForAccountChange(currentUser: User, action: String) =
        sendMail(listOf(currentUser), "accountChange", mapOf(
            "profile" to currentUser,
            "action" to action,
            "publicDomain" to publicDomain))

    fun sendMailForAccountExpiration(users: Collection<User>) =
        users.forEach {
            sendMail(listOf(it), "accountExpiration", mapOf(
                "user" to it,
                "publicDomain" to publicDomain))
        }

    fun sendMailForPasswordReset(currentUser: LdapUserDetails, user: User, newPassword: String) =
        sendMail(listOf(user), "passwordReset", mapOf(
            "currentUser" to currentUser,
            "user" to user,
            "password" to newPassword,
            "publicDomain" to publicDomain))

    fun sendMailForRequestAccessToGroup(currentUser: LdapUserDetails, recipients: List<User>,
                                        adminGroup: Group, wishGroup: Group) =
        sendMail(recipients, "requestAccessToGroup", mapOf(
            "currentUser" to currentUser,
            "requestedGroup" to wishGroup,
            "adminGroup" to adminGroup,
            "publicDomain" to publicDomain))

    fun sendMailForGroupChanged(recipients: List<User>, currentUser: LdapUserDetails, group: Group, user: User, change: Change) =
        sendMail(recipients, "groupChanged", mapOf(
            "currentUser" to currentUser,
            "group" to group,
            "user" to user,
            "publicDomain" to publicDomain,
            "change" to change))

    fun sendNotificationOnUnmaintainedAccounts(recipients: Array<String>, unmaintainedUsers: Map<String, List<User>>) =
        sendMail(recipients, "unmaintainedUsers", mapOf(
            "publicDomain" to publicDomain) + unmaintainedUsers)

    @Throws(PebbleException::class)
    private fun loadTemplate(templateUrl: String, params: Map<String, Any>) =
        with(StringWriter()) {
            pebbleEngine.getTemplate(templateUrl).evaluate(this, params, Locale.ENGLISH)
            toString()
        }

    private fun sendMail(recipients: List<User>, template: String, templateModels: Map<String, Any>) =
        sendMail(recipients.filter { it.mail.isNotBlank() }.map { it.mail }.toTypedArray(),
            template, templateModels)

    private fun sendMail(to: Array<String>, template: String, templateModels: Map<String, Any>): Boolean {
        try {
            val messageID = UUID.randomUUID().toString()
            val context = mapOf("messageID" to messageID) + templateModels

            with(MimeMessageHelper(javaMailSender.createMimeMessage(), "UTF-8")) {
                setFrom(from)
                setReplyTo(reply)
                setSubject(loadTemplate("mail/$template.subject.peb", context))
                setText(loadTemplate("mail/$template.body.peb", context))
                setTo(to)

                if (!logOnly) javaMailSender.send(mimeMessage)
                log.info("[MessageID:{}] E-Mail was successfully sent", messageID)
            }
            return true
        } catch (e: Exception) {
            log.error("Could not sent mail", e)
            return false
        }
    }
}
