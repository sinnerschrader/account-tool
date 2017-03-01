package com.sinnerschrader.s2b.accounttool.config.ldap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;


public class LdapQueries implements InitializingBean
{

	private transient Map<String, String> queries;

	private String searchUser = null;

	private String findUserByUid = null;

	private String findGroupByCn = null;

	private String findGroupsByUser = null;

	private String findAllUsers = null;

	private String listAllGroups = null;

	private String findUserByUidNumber = null;

	private String checkUniqAttribute = null;

	String getQuery(String queryName)
	{
		return queries.get(queryName);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		queries = new LinkedHashMap<>();
		queries.put("searchUser", searchUser);
		queries.put("findUserByUid", findUserByUid);
		queries.put("findGroupByCn", findGroupByCn);
		queries.put("findGroupsByUser", findGroupsByUser);
		queries.put("findAllUsers", findAllUsers);
		queries.put("listAllGroups", listAllGroups);
		queries.put("findUserByUidNumber", findUserByUidNumber);
		queries.put("checkUniqAttribute", checkUniqAttribute);
	}

	public String getSearchUser()
	{
		return searchUser;
	}

	public String getFindUserByUid()
	{
		return findUserByUid;
	}

	public String getFindGroupByCn()
	{
		return findGroupByCn;
	}

	public String getFindGroupsByUser()
	{
		return findGroupsByUser;
	}

	public String getFindAllUsers()
	{
		return findAllUsers;
	}

	public String getListAllGroups()
	{
		return listAllGroups;
	}

	public String getFindUserByUidNumber()
	{
		return findUserByUidNumber;
	}

	public String getCheckUniqAttribute()
	{
		return checkUniqAttribute;
	}

	public void setSearchUser(String searchUser)
	{
		this.searchUser = searchUser;
	}

	public void setFindUserByUid(String findUserByUid)
	{
		this.findUserByUid = findUserByUid;
	}

	public void setFindGroupByCn(String findGroupByCn)
	{
		this.findGroupByCn = findGroupByCn;
	}

	public void setFindGroupsByUser(String findGroupsByUser)
	{
		this.findGroupsByUser = findGroupsByUser;
	}

	public void setFindAllUsers(String findAllUsers)
	{
		this.findAllUsers = findAllUsers;
	}

	public void setListAllGroups(String listAllGroups)
	{
		this.listAllGroups = listAllGroups;
	}

	public void setFindUserByUidNumber(String findUserByUidNumber)
	{
		this.findUserByUidNumber = findUserByUidNumber;
	}

	public void setCheckUniqAttribute(String checkUniqAttribute)
	{
		this.checkUniqAttribute = checkUniqAttribute;
	}

}
