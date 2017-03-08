package com.sinnerschrader.s2b.accounttool.config.ldap;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Management Configuration for handling LDAP Business and cleanup tasks
 */
@ConfigurationProperties(prefix = "ldap-management")
public class LdapManagementConfiguration
{

	private ManagementUser user;

	private List<String> externalUserTypes = null;

	private int leavingUsersInCW = 4;

	public List<String> getExternalUserTypes()
	{
		return externalUserTypes;
	}

	public int getLeavingUsersInCW()
	{
		return leavingUsersInCW;
	}

	public void setLeavingUsersInCW(int leavingUsersInCW)
	{
		this.leavingUsersInCW = leavingUsersInCW;
	}

	public ManagementUser getUser()
	{
		return user;
	}

	public void setExternalUserTypes(List<String> externalUserTypes)
	{
		this.externalUserTypes = externalUserTypes;
	}

	public void setUser(ManagementUser user)
	{
		this.user = user;
	}

	public static class ManagementUser
	{

		private String bindDN;

		private String password;

		public String getBindDN()
		{
			return bindDN;
		}

		public String getPassword()
		{
			return password;
		}

		public void setPassword(String password)
		{
			this.password = password;
		}

		public void setBindDN(String bindDN)
		{
			this.bindDN = bindDN;
		}

	}

	public static class JobsConfiguration
	{

		private boolean active;

		private JobConfiguration updateUnmaintained;

		public boolean isActive()
		{
			return active;
		}

		public JobConfiguration getUpdateUnmaintained()
		{
			return updateUnmaintained;
		}

		public void setActive(boolean active)
		{
			this.active = active;
		}

		public void setUpdateUnmaintained(
			JobConfiguration updateUnmaintained)
		{
			this.updateUnmaintained = updateUnmaintained;
		}
	}

	public static class JobConfiguration
	{

		private boolean active;

		private String cronExpr;

		public boolean isActive()
		{
			return active;
		}

		public String getCronExpr()
		{
			return cronExpr;
		}

		public void setActive(boolean active)
		{
			this.active = active;
		}

		public void setCronExpr(String cronExpr)
		{
			this.cronExpr = cronExpr;
		}

	}

}
