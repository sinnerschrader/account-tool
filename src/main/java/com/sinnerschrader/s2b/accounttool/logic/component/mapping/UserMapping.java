package com.sinnerschrader.s2b.accounttool.logic.component.mapping;

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.SearchResultEntry;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;


/**
 *
 */
public class UserMapping implements ModelMaping<User>
{

	@Autowired
	private LdapConfiguration ldapConfiguration;

	@Override
	public User map(SearchResultEntry entry)
	{
		if (entry == null)
			return null;

		Map.Entry<String, String> company = getCompany(entry.getDN(), entry.getAttributeValue("o"));

		return new User(
			entry.getDN(),
			entry.getAttributeValue("uid"),
			entry.getAttributeValueAsInteger("uidNumber"),
			entry.getAttributeValueAsInteger("gidNumber"),
			entry.getAttributeValue("displayName"),
			entry.getAttributeValue("gecos"),
			entry.getAttributeValue("cn"),
			entry.getAttributeValue("givenName"),
			entry.getAttributeValue("sn"),
			entry.getAttributeValue("homeDirectory"),
			entry.getAttributeValue("loginShell"),
			entry.getAttributeValueAsInteger("szzBirthMonth"),
			entry.getAttributeValueAsInteger("szzBirthDay"),
			entry.getAttributeValue("sambaSID"),
			entry.getAttributeValue("sambaPasswordHistory"),
			entry.getAttributeValue("sambaAcctFlags"),
			entry.getAttributeValue("mail"),
			User.State.fromString(entry.getAttributeValue("szzStatus")),
			User.State.fromString(entry.getAttributeValue("szzMailStatus")),
			entry.getAttributeValueAsLong("sambaPwdLastSet"),
			entry.getAttributeValue("ou"),
			entry.getAttributeValue("description"),
			entry.getAttributeValue("telephoneNumber"),
			entry.getAttributeValue("mobile"),
			entry.getAttributeValue("employeeNumber"),
			entry.getAttributeValue("title"),
			entry.getAttributeValue("l"),
			entry.getAttributeValueAsInteger("szzEntryDay"),
			entry.getAttributeValueAsInteger("szzEntryMonth"),
			entry.getAttributeValueAsInteger("szzEntryYear"),
			entry.getAttributeValueAsInteger("szzExitDay"),
			entry.getAttributeValueAsInteger("szzExitMonth"),
			entry.getAttributeValueAsInteger("szzExitYear"),
			entry.getAttributeValue("szzPublicKey"),
			company.getValue(),
			company.getKey()
		);
	}

	private Map.Entry<String, String> getCompany(String dn, String organization)
	{
		for (Map.Entry<String, String> entry : ldapConfiguration.getCompaniesAsMap().entrySet())
		{
			if (StringUtils.equals(entry.getValue(), organization))
			{
				return entry;
			}
		}
		return extractCompanyFromDn(dn);
	}

	@Deprecated
	private Map.Entry<String, String> extractCompanyFromDn(String dn)
	{
		String[] parts = StringUtils.split(StringUtils.trimToEmpty(dn), ',');
		String key = "";
		if (parts.length == 5)
		{
			key = parts[2];
		}
		final String companyPrefix = "ou=";
		if (StringUtils.startsWith(key, companyPrefix))
		{
			key = StringUtils.replaceOnce(key, companyPrefix, "");
		}
		if (ldapConfiguration.getCompaniesAsMap().containsKey(key))
		{
			final String comKey = key;
			final String comVal = ldapConfiguration.getCompaniesAsMap().get(key);
			return new Map.Entry<String, String>()
			{

				@Override
				public String getKey()
				{
					return comKey;
				}

				@Override
				public String getValue()
				{
					return comVal;
				}

				@Override
				public String setValue(String value)
				{
					throw new IllegalAccessError("You cant set a new value on immutable Object.");
				}
			};
		}
		return null;
	}

	@Override
	public boolean isCompatible(SearchResultEntry entry)
	{
		return (entry != null) && CollectionUtils
			.containsAny(Arrays.asList(entry.getObjectClassValues()), User.objectClasses);
	}

}
