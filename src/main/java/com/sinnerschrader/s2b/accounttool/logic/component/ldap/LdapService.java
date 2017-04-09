package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.sinnerschrader.s2b.accounttool.config.authentication.LdapUserDetails;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.sinnerschrader.s2b.accounttool.logic.exception.BusinessException;
import com.unboundid.ldap.sdk.LDAPConnection;

import java.util.List;

public interface LdapService {

	List<User> getUsers(LDAPConnection connection, int firstResult, int maxResults);

	int getUserCount(LDAPConnection connection);

	User getUserByUid(LDAPConnection connection, String uid);

	List<User> findUserBySearchTerm(LDAPConnection connection, String searchTerm);

	List<Group> getGroupsByUser(LDAPConnection connection, String uid, String userDN);

	List<Group> getGroups(LDAPConnection connection);

	List<String> getEmployeeType(LDAPConnection connection);

	List<String> getLocations(LDAPConnection connection);

	List<String> getDepartments(LDAPConnection connection);

	Group getGroupByCN(LDAPConnection connection, String groupCn);

	List<User> getUsersByGroup(LDAPConnection connection, Group group);

	Group addUserToGroup(LDAPConnection connection, User user, Group group);

	Group removeUserFromGroup(LDAPConnection connection, User user, Group group);

	String resetPassword(LDAPConnection connection, User user) throws BusinessException;

	User activate(LDAPConnection connection, User user);

	User deactivate(LDAPConnection connection, User user);

	User insert(LDAPConnection connection, User user) throws BusinessException;

	User update(LDAPConnection connection, User user) throws BusinessException;

	boolean changePassword(LDAPConnection connection, LdapUserDetails currentUser, String password)
			throws BusinessException;
}
