package com.sinnerschrader.s2b.accounttool.presentation.model

import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.sinnerschrader.s2b.accounttool.presentation.controller.ProfileController
import com.sinnerschrader.s2b.accounttool.presentation.controller.ProfileController.Edit
import com.sinnerschrader.s2b.accounttool.presentation.controller.ProfileController.Edit.NONE
import org.apache.commons.lang3.StringUtils

import java.io.Serializable

class ChangeProfile(
    var edit: Edit = NONE,
    var oldPassword: String = "",
    var password: String = "",
    var passwordRepeat: String = "",
    var telephone: String = "",
    var mobile: String = "",
    var publicKey: String = "",
    var szzExternalAccounts: Map<String,String> = mutableMapOf()
) : Serializable {

    constructor(user: User) : this(
        telephone = user.telephoneNumber,
        mobile = user.mobile,
        publicKey = user.szzPublicKey,
        szzExternalAccounts = user.szzExternalAccounts
    )
}