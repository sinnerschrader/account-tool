package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException;
import com.unboundid.ldap.sdk.LDAPConnection;

import java.util.List;


public interface LdapService
{

	User getUserByUid(LDAPConnection connection, String uid);

	List<User> findUserBySearchTerm(LDAPConnection connection, LdapUserDetails currentUser, String searchTerm);

	List<Group> getGroupsByUser(LDAPConnection connection, String uid, String userDN);

	List<Group> getGroups(LDAPConnection connection, LdapUserDetails currentUser);

	List<String> getEmployeeType(LDAPConnection connection, LdapUserDetails currentUser);

	List<String> getLocations(LDAPConnection connection, LdapUserDetails currentUser);

	List<String> getDepartments(LDAPConnection connection, LdapUserDetails currentUser);

	Group getGroupByCN(LDAPConnection connection, LdapUserDetails currentUser, String groupCn);

	List<User> getUsersByGroup(LDAPConnection connection, LdapUserDetails currentUser, Group group);

	Group addUserToGroup(LDAPConnection connection, LdapUserDetails currentUser, User user, Group group);

	Group removeUserFromGroup(LDAPConnection connection, LdapUserDetails currentUser, User user, Group group);

	String resetPassword(LDAPConnection connection, LdapUserDetails currentUser, User user);

	User activate(LDAPConnection connection, LdapUserDetails currentUser, User user);

	User deactivate(LDAPConnection connection, LdapUserDetails currentUser, User user);

	User insert(LDAPConnection connection, LdapUserDetails currentUser, User user) throws BusinessException;

	User update(LDAPConnection connection, LdapUserDetails currentUser, User user) throws BusinessException;

	boolean changePassword(LDAPConnection connection, LdapUserDetails currentUser, String password);

}
