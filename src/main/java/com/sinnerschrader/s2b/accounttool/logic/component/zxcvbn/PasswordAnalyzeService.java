package com.sinnerschrader.s2b.accounttool.logic.component.zxcvbn;

/**
 * Simple Password validation service interface
 */
public interface PasswordAnalyzeService {

	PasswordValidationResult analyze(String password);
}
