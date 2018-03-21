package com.sinnerschrader.s2b.accounttool.presentation.validation

import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.presentation.model.UserForm
import org.apache.commons.lang3.StringUtils
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmpty
import org.springframework.validation.Validator


@Component(value = "userFormValidator")
class UserFormValidator : Validator {

    private val emailValidator = EmailValidator()

    @Value("\${domain.primary}")
    lateinit var primaryDomain: String

    @Value("\${domain.secondary}")
    lateinit var secondaryDomain: String

    override fun supports(clazz: Class<*>) = UserForm::class.java.isAssignableFrom(clazz)

    override fun validate(target: Any, errors: Errors) {
        arrayOf("firstName",
            "lastName",
            "company",
            "location",
            "title",
            "type",
            "team",
            "status",
            "mailStatus",
            "entryDate",
            "exitDate").map { rejectIfEmpty(errors, it, "required", "$it is required") }
        if (errors.hasErrors()) { return }

        val form = target as UserForm
        if (form.uid.isNotBlank()) {
            val uid = form.uid.trim()
            if(uid.length !in 6..8 || !StringUtils.isAlpha(uid)) {
                errors.rejectValue("uid", "domain", arrayOf<Any>(form.uid),
                    "Please enter a valid username with six, seven or eigth characters")
            }
        }

        if (form.email.isNotBlank()) {
            val email = form.email.trim()
            if (!emailValidator.isValid(email, null)) {
                errors.rejectValue("email", "email", arrayOf<Any>(form.email),
                    "Entered E-Mail is not valid")
            }
            if (!email.endsWith("@$primaryDomain") && !email.endsWith("@$secondaryDomain")) {
                errors.rejectValue("email", "domain", arrayOf<Any>(form.email),
                        "E-Mail has to be part of $primaryDomain Domain")
            }
        }

        if (User.State.fromString(form.status) === User.State.undefined) {
            errors.rejectValue("status", "invalid.status", arrayOf(),
                "Status has an invalid value")
        }
        if (User.State.fromString(form.mailStatus) === User.State.undefined) {
            errors.rejectValue("mailStatus", "invalid.status", arrayOf(),
                "E-Mail Status has an invalid value")
        }

        if (form.birthDate.isNotBlank()) {
            try {
                form.birthAsDate()!!.year
            } catch (e: Exception) {
                errors.rejectValue("birthDate", "date", arrayOf<Any>(form.birthDate),
                    "Birth Date is not a valid date.")
            }

        }

        try {
            form.entryAsDate().year
        } catch (e: Exception) {
            errors.rejectValue("entryDate", "date", arrayOf<Any>(form.entryDate),
                "Entry Date is not a valid date.")
        }

        try {
            form.exitAsDate().year
        } catch (e: Exception) {
            errors.rejectValue("exitDate", "date", arrayOf<Any>(form.exitDate),
                "Exit Date is not a valid date.")
        }

    }
}
