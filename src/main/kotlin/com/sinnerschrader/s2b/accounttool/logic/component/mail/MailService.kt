package com.sinnerschrader.s2b.accounttool.logic.component.mail

import com.sinnerschrader.s2b.accounttool.logic.entity.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.util.*


@Service
class MailService {
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

    fun sendMail(recipients: List<User>, mail: Mail) =
            sendMail(recipients.filter { it.mail.isNotBlank() }.map { it.mail }.toTypedArray(), mail)

    fun sendMail(to: Array<String>, mail: Mail): Boolean {
        try {
            val messageID = UUID.randomUUID().toString()

            with(MimeMessageHelper(javaMailSender.createMimeMessage(), "UTF-8")) {
                setFrom(from)
                setReplyTo(reply)
                setSubject(mail.subject)
                setText(mail.body + "\n[ReqID:$messageID]")
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

    companion object {
        private val log = LoggerFactory.getLogger(MailService::class.java)
    }
}
