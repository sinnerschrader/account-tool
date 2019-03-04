package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException

@RestController
@RequestMapping("/v2")
class UserControllerV2 {
    @Autowired
    lateinit var ldapServiceV2: LdapServiceV2

    @GetMapping("/user")
    fun getUser(@RequestParam(required = false) state: State?,
                @RequestParam(required = false) search: String?) =
            ldapServiceV2.getUser(state = state, search = search)

    @GetMapping("/user/{uid}")
    fun getUser(@PathVariable uid: String) = ldapServiceV2.getUser(uid = uid).single()

    @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Not found")  // 404
    @ExceptionHandler(NoSuchElementException::class)
    fun notFound() {}

    @ResponseStatus(value=HttpStatus.CONFLICT, reason="Data integrity violation")  // 409
    @ExceptionHandler(IllegalArgumentException::class)
    fun conflict() {}
}
