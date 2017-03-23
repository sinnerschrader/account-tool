package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.logic.entity.User;

import java.util.List;

/**
 * LDAP Business Service for scheduling tasks and perform cleanups.
 */
public interface LdapBusinessService {

	List<User> getUnmaintainedExternals();

	List<User> getLeavingUsers();

	List<User> getUnmaintainedMailUsers();

	void addDefaultGroups(User user);

	void delDefaulGroups(User user);
}
