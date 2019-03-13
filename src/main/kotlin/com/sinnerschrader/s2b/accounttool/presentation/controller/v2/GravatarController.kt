package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.util.DigestUtils
import org.springframework.web.bind.annotation.*

@RestController
@Api(tags = ["Gravatar"], description = "Provides access to user avatar.")
@RequestMapping("/v2")
class GravatarController {
    @Value("\${gravatar.domain}")
    private lateinit var gravatarDomain: String

    @Value("\${gravatar.path}")
    lateinit var gravatarPath: String

    @Autowired
    lateinit var ldapServiceV2: LdapServiceV2

    @ApiOperation("Retrieve avatar")
    @GetMapping("/gravatar/{uid}")
    fun url(@PathVariable uid: String, @RequestParam(name = "s" ,required = false) size: Int?) =
            with(ldapServiceV2.getUser(uid).single()){
                DigestUtils.md5DigestAsHex(mail.toLowerCase().trim().toByteArray()). let {
                    "$gravatarDomain$gravatarPath/$it?s=${size ?: 96}"
                }
            }
}