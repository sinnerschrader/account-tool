package com.sinnerschrader.s2b.accounttool.logic.component.mapping;

import com.unboundid.ldap.sdk.SearchResultEntry;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Mapping interface for transfering LDAP Attributes into a POJO
 */
public interface ModelMaping<T extends Comparable<T>> {

	T map(SearchResultEntry entry);

	default List<T> map(List<SearchResultEntry> entries) {
		List<T> result = new LinkedList<>();
		entries.forEach(
				entry -> {
					if (isCompatible(entry)) {
						result.add(map(entry));
					}
				});
		Collections.sort(result);
		return result;
	}

	boolean isCompatible(SearchResultEntry entry);
}
