package com.sinnerschrader.s2b.accounttool.presentation.model;

import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Form Bean for handling Profile changes
 */
public class ChangeProfile implements Serializable {

	private String changePublicKey;

	private String changePhones;

	private String changePassword;

	private String oldPassword;

	private String password;

	private String passwordRepeat;

	private String telephone;

	private String mobile;

	private String publicKey;

	public ChangeProfile() {
		this.changePublicKey = "";
		this.changePhones = "";
		this.changePassword = "";
		this.oldPassword = "";
		this.password = "";
		this.passwordRepeat = "";
		this.telephone = "";
		this.mobile = "";
		this.publicKey = "";
	}

	public ChangeProfile(User user) {
		this();
		this.telephone = user.getTelephoneNumber();
		this.mobile = user.getMobile();
		this.publicKey = user.getSzzPublicKey();
	}

	public User createUserEntityFromForm(User persistentUser) {
		return new User(
				persistentUser.getDn(),
				persistentUser.getUid(),
				persistentUser.getUidNumber(),
				persistentUser.getGidNumber(),
				persistentUser.getDisplayName(),
				persistentUser.getGecos(),
				persistentUser.getCn(),
				persistentUser.getGivenName(),
				persistentUser.getSn(),
				persistentUser.getHomeDirectory(),
				persistentUser.getLoginShell(),
				persistentUser.getBirthDate(),
				persistentUser.getSambaSID(),
				persistentUser.getSambaPasswordHistory(),
				persistentUser.getSambaAcctFlags(),
				persistentUser.getMail(),
				persistentUser.getSzzStatus(),
				persistentUser.getSzzMailStatus(),
				persistentUser.getSambaPwdLastSet(),
				persistentUser.getEmployeeEntryDate(),
				persistentUser.getEmployeeExitDate(),
				persistentUser.getOu(),
				persistentUser.getDescription(),
				StringUtils.trimToNull(telephone),
				StringUtils.trimToNull(mobile),
				persistentUser.getEmployeeNumber(),
				persistentUser.getTitle(),
				persistentUser.getL(),
				StringUtils.trimToNull(publicKey),
				persistentUser.getO(),
				persistentUser.getCompanyKey());
	}

	public boolean isPasswordChange() {
		return StringUtils.isNotBlank(changePassword);
	}

	public boolean isPhoneChange() {
		return StringUtils.isNotBlank(changePhones);
	}

	public boolean isPublicKeyChange() {
		return StringUtils.isNotBlank(changePublicKey);
	}

	public String getChangePublicKey() {
		return changePublicKey;
	}

	public void setChangePublicKey(String changePublicKey) {
		this.changePublicKey = changePublicKey;
	}

	public String getChangePhones() {
		return changePhones;
	}

	public void setChangePhones(String changePhones) {
		this.changePhones = changePhones;
	}

	public String getChangePassword() {
		return changePassword;
	}

	public void setChangePassword(String changePassword) {
		this.changePassword = changePassword;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordRepeat() {
		return passwordRepeat;
	}

	public void setPasswordRepeat(String passwordRepeat) {
		this.passwordRepeat = passwordRepeat;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
}
