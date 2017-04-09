package com.sinnerschrader.s2b.accounttool.logic.component.encryption;

import org.springframework.security.crypto.codec.Hex;
import sun.security.provider.MD4;

import java.io.UnsupportedEncodingException;

/**
 * Simple Samba Passwort Encrypter. This Encrypter is very unsecure, because it hashes the Password
 * on MD4.
 */
@SuppressWarnings("deprecated")
public class SambaEncrypter implements Encrypter {

	@Override
	public String encrypt(String password) {
		if (password == null || "".equals(password.trim())) {
			throw new IllegalArgumentException("Password can't be null or empty");
		}
		try {
			final String charsetName = "UTF-16LE";
			return new String(Hex.encode(MD4.getInstance().digest(password.getBytes(charsetName))))
					.toUpperCase();
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("Could not generate samba password hash", uee);
		}
	}
}
