package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import com.sinnerschrader.s2b.accounttool.config.WebConstants
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.LdapService
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2.UserAttributes.FULL
import com.sinnerschrader.s2b.accounttool.logic.component.ldap.v2.LdapServiceV2.UserAttributes.INFO
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State
import com.sinnerschrader.s2b.accounttool.logic.entity.User.State.inactive
import com.sinnerschrader.s2b.accounttool.presentation.controller.v2.DynamicAllowedValues.Companion.COMPANIES
import com.sinnerschrader.s2b.accounttool.presentation.controller.v2.DynamicAllowedValues.Companion.USERTYPE
import com.unboundid.ldap.sdk.LDAPConnection
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore
import java.time.LocalDate

@RestController
@Api(tags = ["Admin"], description = "Provides admin access")
@RequestMapping("/v2")
@PreAuthorize("@authorizationService.ensureUserAdministration()")
class AdminController {
    @Autowired
    lateinit var ldapServiceV2: LdapServiceV2

    @Autowired
    lateinit var ldapService: LdapService

    @ApiOperation("Retrieve users")
    @GetMapping("/admin/user")
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
                @RequestParam(required = false) @DateTimeFormat(iso = DATE) exitDateEnd: LocalDate?,
                @ApiParam(allowableValues = COMPANIES) company: String?,
                @ApiParam(allowableValues = USERTYPE) type: String?) =
            ldapServiceV2.getUser(state = state,
                    company = company,
                    type = type,
                    searchTerm = searchTerm,
                    entryDateRange = LdapServiceV2.DateRange.of(entryDateStart, entryDateEnd),
                    exitDateRange = LdapServiceV2.DateRange.of(exitDateStart, exitDateEnd),
                    attributes = FULL)

    @ApiOperation("Retrieve user by uid")
    @GetMapping("/admin/user/{uid}")
    fun getUser(@PathVariable uid: String) = ldapServiceV2.getUser(uid = uid, attributes = FULL).single()


    @ApiOperation("Retrieve inactive group members")
    @GetMapping("/admin/maintenance/group/inactive")
    fun getMaintenanceGroupInactive() =
            (ldapServiceV2.getUser(state = inactive, attributes = INFO).map { it.uid }).let { inactive ->
                ldapServiceV2.getGroups().map {
                    it.name to it.members.apply { retainAll(inactive) }
                }.toMap().filterValues { it.isNotEmpty() }
            }

    @ApiOperation("Remove inactive group members")
    @DeleteMapping("/admin/maintenance/group/inactive")
    fun deleteMaintenanceGroupInactive(@ApiIgnore
                                       @RequestAttribute(name = WebConstants.ATTR_CONNECTION)
                                       connection: LDAPConnection) =
            // TODO optimise performance (batch change)
            ldapServiceV2.getUser(state = inactive, attributes = FULL).forEach {
                ldapService.clearGroups(connection, it)
            }
}
