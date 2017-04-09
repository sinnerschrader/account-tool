package com.sinnerschrader.s2b.accounttool.config.ldap;

import org.springframework.beans.factory.InitializingBean;

import java.util.LinkedHashMap;
import java.util.Map;

public class LdapQueries implements InitializingBean {

	private transient Map<String, String> queries;

	private String searchUser = null;

	private String findUserByUid = null;

	private String findGroupByCn = null;

	private String findGroupsByUser = null;

	private String listAllGroups = null;

	private String listAllUsers = null;

	private String findUserByUidNumber = null;

	private String checkUniqAttribute = null;

	String getQuery(String queryName) {
		return queries.get(queryName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		queries = new LinkedHashMap<>();
		queries.put("searchUser", searchUser);
		queries.put("findUserByUid", findUserByUid);
		queries.put("findGroupByCn", findGroupByCn);
		queries.put("findGroupsByUser", findGroupsByUser);
		queries.put("listAllUsers", listAllUsers);
		queries.put("listAllGroups", listAllGroups);
		queries.put("findUserByUidNumber", findUserByUidNumber);
		queries.put("checkUniqAttribute", checkUniqAttribute);
	}

	public String getSearchUser() {
		return searchUser;
	}

	public String getFindUserByUid() {
		return findUserByUid;
	}

	public String getFindGroupByCn() {
		return findGroupByCn;
	}

	public String getFindGroupsByUser() {
		return findGroupsByUser;
	}

	public String getListAllGroups() {
		return listAllGroups;
	}

	public String getFindUserByUidNumber() {
		return findUserByUidNumber;
	}

	public String getCheckUniqAttribute() {
		return checkUniqAttribute;
	}

	public void setSearchUser(String searchUser) {
		this.searchUser = searchUser;
	}

	public void setFindUserByUid(String findUserByUid) {
		this.findUserByUid = findUserByUid;
	}

	public void setFindGroupByCn(String findGroupByCn) {
		this.findGroupByCn = findGroupByCn;
	}

	public void setFindGroupsByUser(String findGroupsByUser) {
		this.findGroupsByUser = findGroupsByUser;
	}

	public void setListAllGroups(String listAllGroups) {
		this.listAllGroups = listAllGroups;
	}

	public void setFindUserByUidNumber(String findUserByUidNumber) {
		this.findUserByUidNumber = findUserByUidNumber;
	}

	public void setCheckUniqAttribute(String checkUniqAttribute) {
		this.checkUniqAttribute = checkUniqAttribute;
	}

	public String getListAllUsers() {
		return listAllUsers;
	}

	public void setListAllUsers(String listAllUsers) {
		this.listAllUsers = listAllUsers;
	}
}
