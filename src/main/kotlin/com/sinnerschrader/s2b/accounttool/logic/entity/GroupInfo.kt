package com.sinnerschrader.s2b.accounttool.logic.entity

import java.util.*

data class GroupInfo (
        val name: String,
        val description: String,
        val members: SortedSet<String>
)
