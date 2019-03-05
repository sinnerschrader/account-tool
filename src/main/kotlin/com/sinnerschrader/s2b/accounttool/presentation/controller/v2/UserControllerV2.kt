package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
import java.time.LocalDate

@RestController
@RequestMapping("/v2")
class UserControllerV2 {
    @Autowired
    lateinit var ldapServiceV2: LdapServiceV2

    @GetMapping("/user")
    fun getUser(@ApiParam("account state")
                @RequestParam(required = false) state: State?,
                @ApiParam("search multiple fields for a specific keyword")
                @RequestParam(required = false) searchTerm: String?,
                @ApiParam("earliest entry date")
                @RequestParam(required = false) @DateTimeFormat(iso = DATE) entryDateStart: LocalDate?,
                @ApiParam("latest entry date")
                @RequestParam(required = false) @DateTimeFormat(iso = DATE) entryDateEnd: LocalDate?,
                @ApiParam("earliest exit date")
                @RequestParam(required = false) @DateTimeFormat(iso = DATE) exitDateStart: LocalDate?,
                @ApiParam("latest exit date")
                @RequestParam(required = false) @DateTimeFormat(iso = DATE) exitDateEnd: LocalDate?) =
            ldapServiceV2.getUser(state = state,
                    searchTerm = searchTerm,
                    entryDateRange = LdapServiceV2.DateRange.of(entryDateStart, entryDateEnd),
                    exitDateRange = LdapServiceV2.DateRange.of(exitDateStart, exitDateEnd))


    @GetMapping("/user/{uid}")
    fun getUser(@PathVariable uid: String) = ldapServiceV2.getUser(uid = uid).single()
}
