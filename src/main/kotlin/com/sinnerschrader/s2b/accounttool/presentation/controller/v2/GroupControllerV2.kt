package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@Api(tags = ["Group"], description = "Provides access to groups")
@RequestMapping("/v2")
class GroupControllerV2 {
    @Autowired
    lateinit var ldapServiceV2: LdapServiceV2

    @ApiOperation("Retrieve groups")
    @GetMapping("/group")
    fun getGroups(@ApiParam("filter by memberUid") @RequestParam(required = false) memberUid: String? = null) = ldapServiceV2.getGroups(memberUid = memberUid)

    @ApiOperation("Retrieve group by name")
    @GetMapping("/group/{cn}")
    fun getGroup(@PathVariable cn: String) = ldapServiceV2.getGroups(cn = cn).single()
}
