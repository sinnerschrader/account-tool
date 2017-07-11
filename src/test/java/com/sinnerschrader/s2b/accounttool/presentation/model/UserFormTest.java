package com.sinnerschrader.s2b.accounttool.presentation.model;

import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDate;


/**
 * Unit Test for validatating population of UserForm
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles({"test"})
public class UserFormTest {

    @Test
    public void testCreateUserForm() {
        final LocalDate entryDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        final LocalDate exitDate = entryDate.plusYears(50).minusDays(1);

        UserForm userForm = new UserForm();
        Assert.assertTrue(userForm.entryAsDate().equals(entryDate));
        Assert.assertTrue(userForm.exitAsDate().equals(exitDate));
    }

    @Test
    public void testEditUserForm() {
        User user = new User("uid=exampl,ou=users,dc=example,dc=com",
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
            LocalDate.of(1972, 07, 01),
            "SID-12-12-12",
            "000000",
            "[U    ]",
            "example.user@sinnerschrader.com",
            User.State.active,
            User.State.active,
            1490260799L,
            LocalDate.of(1999, 01, 01),
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
        );
        UserForm userForm = new UserForm(user);
        Assert.assertTrue(userForm.entryAsDate().equals(user.getEmployeeEntryDate()));
        Assert.assertTrue(userForm.exitAsDate().equals(user.getEmployeeExitDate()));
        Assert.assertEquals(userForm.getEmail(), user.getMail());
        Assert.assertEquals(userForm.getStatus(), user.getSzzStatus().name());
        Assert.assertEquals(userForm.getMailStatus(), user.getSzzMailStatus().name());
        Assert.assertEquals(userForm.getUid(), user.getUid());
    }

}
