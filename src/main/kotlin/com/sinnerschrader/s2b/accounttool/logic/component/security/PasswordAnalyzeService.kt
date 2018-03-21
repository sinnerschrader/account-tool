package com.sinnerschrader.s2b.accounttool.logic.component.security

import com.nulabinc.zxcvbn.Feedback
import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.Zxcvbn
import org.springframework.stereotype.Service
import java.util.*


@Service
object PasswordAnalyzeService {
    class PasswordValidationResult(strength: Strength, feedback: Feedback) {
        val isValid = strength.score >= 3
        val errorCodes = feedback.suggestions + listOfNotNull(feedback.warning).filter { it.isNotBlank() }
    }

    fun analyze(password: String): PasswordValidationResult {
        val strength = Zxcvbn().measure(password)
        val feedback = strength.feedback.withResourceBundle(object : ResourceBundle() {
            override fun handleGetObject(key: String): Any = key
            override fun getKeys(): Enumeration<String>? = null
        })
        return PasswordValidationResult(strength, feedback)
    }
}
