package com.sinnerschrader.s2b.accounttool.presentation.validation

import com.sinnerschrader.s2b.accounttool.config.DomainConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.presentation.model.UserForm
import org.apache.commons.lang3.StringUtils
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmpty
import org.springframework.validation.Validator


@Component(value = "userFormValidator")
class UserFormValidator : Validator {

    private val emailValidator = EmailValidator()

    @Autowired
    private lateinit var domainConfiguration: DomainConfiguration

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

        if (form.emailPrefix.isNotBlank()) {
            val email = form.emailPrefix.trim()
            if (!emailValidator.isValid("$email@${domainConfiguration.mailDomain(form.type)}", null)) {
                errors.rejectValue("emailPrefix", "email", arrayOf<Any>(form.emailPrefix),
                    "Entered E-Mail is not valid")
            }
        }

        if (form.birthDate.isNotBlank()) {
            try {
                form.birthAsDate()
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
