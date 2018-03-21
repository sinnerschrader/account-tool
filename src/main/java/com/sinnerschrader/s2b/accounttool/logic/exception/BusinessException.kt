package com.sinnerschrader.s2b.accounttool.logic.exception

@Deprecated("remove")
class BusinessException : Exception {
    val code: String
    val args: Array<Any>?

    @JvmOverloads constructor(msg: String, code: String, args: Array<Any>? = null) : super(msg) {
        this.args = args
        this.code = code
    }

    constructor(msg: String, code: String, t: Throwable) : this(msg, code, null, t)

    constructor(msg: String, code: String, args: Array<Any>?, t: Throwable) : super(msg, t) {
        this.args = args
        this.code = code
    }
}
