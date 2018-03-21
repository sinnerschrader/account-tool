package com.sinnerschrader.s2b.accounttool.presentation.model

import com.sinnerschrader.s2b.accounttool.logic.entity.User
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import java.time.LocalDate


/**
 * Unit Test for validatating population of UserForm
 */
@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest
@ActiveProfiles("test")
class UserFormTest {

    @Test
    fun testCreateUserForm() {
        val entryDate = LocalDate.now().plusMonths(1).withDayOfMonth(1)
        val exitDate = entryDate.plusYears(50).minusDays(1)

        val userForm = UserForm()
        Assert.assertTrue(userForm.entryAsDate() == entryDate)
        Assert.assertTrue(userForm.exitAsDate() == exitDate)
    }

    @Test
    fun testEditUserForm() {
        val user = User("uid=exampl,ou=users,dc=example,dc=com",
                "exampl",
                65353,
                1000,
                "Example User",
                "Example User",
                "Example User",
                "Example",
                "User",
                "/dev/null",
                "/bin/false",
                LocalDate.of(1972, 7, 1),
                "SID-12-12-12",
                "000000",
                "[U    ]",
                "example.user@sinnerschrader.com",
                User.State.active,
                User.State.active,
                1490260799L,
                LocalDate.of(1999, 1, 1),
                LocalDate.of(2018, 12, 31),
                "Technik",
                "Mitarbeiter",
                "+49 40 123 456 56789",
                "+49 1234 567 8901",
                "123123123123-123123123123-123123123213",
                "Example User",
                "Hamburg",
                "",
                "Example Company",
                "e1c1",
                "",
                ""
        )
        val userForm = UserForm(user)
        Assert.assertTrue(userForm.entryAsDate() == user.employeeEntryDate)
        Assert.assertTrue(userForm.exitAsDate() == user.employeeExitDate)
        Assert.assertEquals(userForm.email, user.mail)
        Assert.assertEquals(userForm.status, user.szzStatus.name)
        Assert.assertEquals(userForm.mailStatus, user.szzMailStatus.name)
        Assert.assertEquals(userForm.uid, user.uid)
    }

}
