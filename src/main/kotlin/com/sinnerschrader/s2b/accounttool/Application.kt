package com.sinnerschrader.s2b.accounttool

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@EnableScheduling
class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
