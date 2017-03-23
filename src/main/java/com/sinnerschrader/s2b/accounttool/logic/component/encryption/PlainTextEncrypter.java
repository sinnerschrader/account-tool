package com.sinnerschrader.s2b.accounttool.logic.component.encryption;

/**
 * Plaintext Crypter with NO encryption for testing purpose only
 */
public class PlainTextEncrypter implements Encrypter {

	@Override
	public String encrypt(String password) {
		return password != null ? password.trim() : "";
	}
}
