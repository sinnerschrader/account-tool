package com.sinnerschrader.s2b.accounttool.presentation.controller.v2

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@Controller
object ExceptionHandlers {
    @ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Not found")  // 404
    @ExceptionHandler(NoSuchElementException::class)
    fun notFound() {}

    @ResponseStatus(value= HttpStatus.CONFLICT, reason="Data integrity violation")  // 409
    @ExceptionHandler(IllegalArgumentException::class)
    fun conflict() {}
}
