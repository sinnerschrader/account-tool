package com.sinnerschrader.s2b.accounttool.presentation.messaging

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest


@Deprecated("remove")
@Component(value = "globalMessageFactory")
class GlobalMessageFactory {

    @Autowired
    private val messageSource: MessageSource? = null

    fun create(type: GlobalMessageType, messageKey: String, vararg args: String): GlobalMessage {
        return GlobalMessage(messageKey,
                messageSource!!.getMessage(messageKey, args, Locale.ENGLISH), type)
    }

    fun createError(messageKey: String, vararg args: String): GlobalMessage {
        return create(GlobalMessageType.ERROR, messageKey, *args)
    }

    fun createInfo(messageKey: String, vararg args: String): GlobalMessage {
        return create(GlobalMessageType.INFO, messageKey, *args)
    }

    fun store(request: HttpServletRequest, message: GlobalMessage) {
        val session = request.session
        if (session.getAttribute(SESSION_KEY) == null) {
            session.setAttribute(SESSION_KEY, LinkedList<GlobalMessage>())
        }
        val messages = session.getAttribute(SESSION_KEY) as MutableList<GlobalMessage>
        messages.add(message)
    }

    fun pop(request: HttpServletRequest): List<GlobalMessage> {
        val session = request.session
        if (session.getAttribute(SESSION_KEY) == null) {
            return ArrayList()
        }
        val messages = session.getAttribute(SESSION_KEY) as List<GlobalMessage>
        session.removeAttribute(SESSION_KEY)
        return messages
    }

    companion object {
        private val SESSION_KEY = GlobalMessageFactory::class.java.name
    }
}
