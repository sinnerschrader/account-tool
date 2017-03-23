package com.sinnerschrader.s2b.accounttool.logic.component.encryption;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"test"})
public class PasswordCrypterTests {

	@Test
	public void testPasswordEncryption() {
		PasswordEncrypter crypter = new PasswordEncrypter();
		String salt = RandomStringUtils.randomAlphanumeric(16);
		String password = RandomStringUtils.randomAlphanumeric(32);

		Assert.assertEquals(crypter.encrypt(password, salt), crypter.encrypt(password, salt));
		Assert.assertNotEquals(password, crypter.encrypt(password));
		Assert.assertNotEquals(crypter.encrypt(password), crypter.encrypt(password));
	}
}
