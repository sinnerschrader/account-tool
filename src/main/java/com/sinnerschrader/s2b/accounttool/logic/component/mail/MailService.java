package com.sinnerschrader.s2b.accounttool.logic.component.mail;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;

import java.util.List;
import java.util.Map;

/**
 * Mail Service Interface for sending mails
 */
public interface MailService {

	boolean sendMailForAccountChange(User currentUser, String event);

	boolean sendMailForPasswordReset(LdapUserDetails currentUser, User user, String newPassword);

	boolean sendNotificationOnUnmaintainedAccounts(
			String[] receipients, Map<String, List<User>> unmaintainedUsers);

	boolean sendMailForRequestAccessToGroup(
			LdapUserDetails currentUser, List<User> receipients, Group adminGroup, Group wishGroup);
}
