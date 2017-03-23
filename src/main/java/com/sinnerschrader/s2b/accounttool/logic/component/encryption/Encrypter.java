package com.sinnerschrader.s2b.accounttool.logic.component.encryption;

/** */
public interface Encrypter {

	/**
	 * Encrypts the provides input. The algorithm is based on the Implementation.
	 *
	 * @param password plain text password
	 * @return encrypted password
	 */
	String encrypt(String password);
}
