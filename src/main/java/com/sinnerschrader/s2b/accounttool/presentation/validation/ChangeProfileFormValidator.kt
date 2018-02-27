package com.sinnerschrader.s2b.accounttool.presentation.validation

import com.sinnerschrader.s2b.accounttool.logic.component.security.PwnedPasswordService
import com.sinnerschrader.s2b.accounttool.logic.component.security.PasswordAnalyzeService
import com.sinnerschrader.s2b.accounttool.presentation.RequestUtils
import com.sinnerschrader.s2b.accounttool.presentation.model.ChangeProfile
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import org.apache.sshd.common.util.buffer.Buffer
import org.apache.sshd.common.util.buffer.ByteArrayBuffer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import org.springframework.validation.Validator
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

@Component(value = "changeProfileFormValidator")
class ChangeProfileFormValidator : Validator {

    @Autowired
    private lateinit var passwordAnalyzeService: PasswordAnalyzeService

    override fun supports(clazz: Class<*>) =ChangeProfile::class.java.isAssignableFrom(clazz)

    private fun validateAndSanitizeSSHKey(form: ChangeProfile, errors: Errors) {
        val formAttribute = "publicKey"

        rejectIfEmptyOrWhitespace(errors, "publicKey", "required")
        if (errors.hasErrors()) return

        var sanitizedKey = ""
        val parts = StringUtils.split(form.publicKey, " ")
        if (parts.size >= 2) {
            try {
                val base64Key = parts[1]
                val binary = Base64.decodeBase64(base64Key)
                val publicKey = ByteArrayBuffer(binary).rawPublicKey
                if (StringUtils.equals(publicKey.algorithm, "EC")) {
                    val ecKey = publicKey as ECPublicKey
                    if (ecKey.params.order.bitLength() < 256) {
                        errors.rejectValue(formAttribute, "publicKey.ec.insecure",
                                "ec key with minimum of 256 bit required")
                        return
                    }
                    sanitizedKey = "ecdsa-sha2-nistp" + ecKey.params.order.bitLength() + " "
                } else if (StringUtils.equals(publicKey.algorithm, "RSA")) {
                    val rsaPublicKey = publicKey as RSAPublicKey
                    if (rsaPublicKey.modulus.bitLength() < 2048) {
                        errors.rejectValue(formAttribute, "publicKey.rsa.insecure",
                                "rsa key with minimum of 2048 bit required")
                        return
                    }
                    sanitizedKey = "ssh-rsa "
                } else if (StringUtils.equals(publicKey.algorithm, "DSA")) {
                    val dsaPublicKey = publicKey as DSAPublicKey
                    if (dsaPublicKey.params.p.bitLength() < 2048) {
                        errors.rejectValue(formAttribute, "publicKey.dsa.insecure",
                                "dsa key with minimum of 2048 bit required")
                        return
                    }
                    sanitizedKey = "ssh-dss "
                }
                val buffer = ByteArrayBuffer()
                buffer.putRawPublicKey(publicKey)
                sanitizedKey += Base64.encodeBase64String(buffer.compactData)
                form.publicKey = sanitizedKey
            } catch (e: Exception) {
                val msg = "Could not parse public key"
                errors.rejectValue(formAttribute, "publicKey.invalid",
                        "The defined public can't be parsed")
                LOG.error(msg)
                if (LOG.isDebugEnabled) {
                    LOG.error(msg, e)
                }
            }

        } else {
            errors.rejectValue(formAttribute, "publicKey.invalid", "The defined public can't be parsed")
        }
    }

    private fun validatePassword(form: ChangeProfile, errors: Errors) {
        rejectIfEmptyOrWhitespace(errors, "oldPassword", "required",
                "Please enter your current password")
        rejectIfEmptyOrWhitespace(errors, "password", "required",
                "Please enter a password")
        if (errors.hasErrors()) return

        rejectIfEmptyOrWhitespace(errors, "passwordRepeat", "required",
                "Please repeat the password")
        if (errors.hasErrors()) return

        val currentUser = RequestUtils.getCurrentUserDetails()
        if (currentUser.password != form.oldPassword) {
            errors.rejectValue("oldPassword", "password.previous.notMatch")
        }
        if (currentUser.password == form.password) {
            errors.rejectValue("password", "password.noChange")
        }

        val result = passwordAnalyzeService.analyze(form.password)
        if (!result.isValid) {
            val codes = result.errorCodes
            if (codes.isEmpty()) {
                // this is a fallback, if no message and no suggestions are provided
                errors.rejectValue("password", "passwordValidation.general.error", "The password is too weak")
            } else {
                for (code in result.errorCodes) {
                    errors.rejectValue("password", code, "Your password violates a rule.")
                }
            }
        } else if (PwnedPasswordService.isPwned(form.password)) {
            errors.rejectValue("password", "passwordValidation.isPwned.error",
                    "The password has previously appeared in a security breach")
        }
    }

    override fun validate(target: Any, errors: Errors) {
        with(target as ChangeProfile){
            when{
                isPublicKeyChange() -> validateAndSanitizeSSHKey(this, errors)
                isPhoneChange() -> Unit // disabled check
                isPasswordChange() -> validatePassword(this, errors)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ChangeProfileFormValidator::class.java)
    }
}
