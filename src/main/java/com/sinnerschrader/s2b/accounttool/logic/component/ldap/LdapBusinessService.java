package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.logic.entity.User;


/**
 * LDAP Business Service for scheduling tasks and perform cleanups.
 */
public interface LdapBusinessService
{

	void getUnmaintainedExternals();

	void addDefaultGroups(User user);

	void delDefaulGroups(User user);

}
