package com.sinnerschrader.s2b.accounttool.presentation.messaging

import java.io.Serializable

@Deprecated("remove")
class GlobalMessage internal constructor(val key: String, val text: String, val type: GlobalMessageType) : Serializable
