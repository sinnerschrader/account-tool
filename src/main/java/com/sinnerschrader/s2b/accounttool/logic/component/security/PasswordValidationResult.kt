package com.sinnerschrader.s2b.accounttool.logic.component.security

import com.nulabinc.zxcvbn.Feedback
import com.nulabinc.zxcvbn.Strength

class PasswordValidationResult(strength: Strength, feedback: Feedback) {
    val isValid = strength.score >= 3
    val errorCodes = feedback.suggestions + if (feedback.warning.isNotBlank()) listOf(feedback.warning) else emptyList()
}
