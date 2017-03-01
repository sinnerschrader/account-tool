package com.sinnerschrader.s2b.accounttool.logic.component.mapping;

import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.GroupOfNames;
import com.sinnerschrader.s2b.accounttool.logic.entity.PosixGroup;
import com.unboundid.ldap.sdk.SearchResultEntry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 *
 */
public class GroupMapping implements ModelMaping<Group>
{

	@Override
	public Group map(SearchResultEntry entry)
	{
		if (entry == null)
			return null;
		final List<String> objectClasses = Arrays.asList(entry.getObjectClassValues());
		if (isPosixGroup(objectClasses))
		{
			return new PosixGroup(
				entry.getDN(),
				entry.getAttributeValue("cn"),
				entry.getAttributeValueAsInteger("gid"),
				entry.getAttributeValue("description"),
				entry.getAttributeValues("memberUid")
			);
		}
		final boolean unique = isGroupOfUniqueNames(objectClasses);
		if (unique || isGroupOfNames(objectClasses))
		{
			final String memberAttribute = unique
				? Group.GroupType.GroupOfUniqueNames.getMemberAttritube()
				: Group.GroupType.GroupOfNames.getMemberAttritube();

			return new GroupOfNames(
				entry.getDN(),
				entry.getAttributeValue("cn"),
				entry.getAttributeValue("description"),
				unique,
				entry.getAttributeValues(memberAttribute)
			);
		}
		throw new IllegalArgumentException("Provided result entry is not supported. Please call isCompatible before.");
	}

	private boolean isGroupOfNames(Collection<String> objectClasses)
	{
		return objectClasses.contains(Group.GroupType.GroupOfNames.getObjectClass());
	}

	private boolean isGroupOfUniqueNames(Collection<String> objectClasses)
	{
		return objectClasses.contains(Group.GroupType.GroupOfUniqueNames.getObjectClass());
	}

	private boolean isPosixGroup(Collection<String> objectClasses)
	{
		return objectClasses.contains(Group.GroupType.Posix.getObjectClass());
	}

	@Override
	public boolean isCompatible(SearchResultEntry entry)
	{
		if (entry == null)
		{
			return false;
		}
		final List<String> objectClasses = Arrays.asList(entry.getObjectClassValues());
		return isPosixGroup(objectClasses) ||
			isGroupOfNames(objectClasses) ||
			isGroupOfUniqueNames(objectClasses);
	}

}
