package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v2")
class GroupControllerV2 {
    @Autowired
    lateinit var ldapServiceV2: LdapServiceV2

    @GetMapping("/group")
    fun getGroups(@RequestParam(required = false) memberUid: String? = null) = ldapServiceV2.getGroups(memberUid = memberUid)

    @GetMapping("/group/{cn}")
    fun getGroup(@PathVariable cn: String) = ldapServiceV2.getGroups(cn = cn).single()
}
