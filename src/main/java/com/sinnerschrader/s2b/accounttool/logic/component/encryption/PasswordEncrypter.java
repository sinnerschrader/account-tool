package com.sinnerschrader.s2b.accounttool.logic.component.encryption;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * This PasswordEncrypter is a simple implementation for salted SHA-512 password hashes in a
 * compatible format
 */
public class PasswordEncrypter implements Encrypter {

	@Override
	public String encrypt(String password) {
		return encrypt(password, RandomStringUtils.randomAlphanumeric(16));
	}

	public String encrypt(String password, String salt) {
		if (password == null || "".equals(password.trim()) || salt == null || "".equals(salt.trim())) {
			throw new IllegalArgumentException("Password and salt can't be null or empty");
		}
		final String saltPrefix = "$6$";
		final String passwordHashPrefix = "{CRYPT}";

		return passwordHashPrefix + Crypt.crypt(password, saltPrefix + salt);
	}
}
