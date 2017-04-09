package com.sinnerschrader.s2b.accounttool.logic.component.licences;

import org.apache.commons.lang3.StringUtils;

import java.beans.Transient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by vikgru on 19/01/2017.
 */
public class Dependency implements Comparable<Dependency> {

	private final String groupId;

	private final String artifactId;

	private final String version;

	private final List<License> licenses;

	Dependency(String groupId, String artifactId, String version, List<License> licenses) {
		this.artifactId = artifactId;
		this.groupId = groupId;
		this.version = version;
		this.licenses = Collections.unmodifiableList(licenses);
	}

	Dependency(String groupId, String artifactId, String version, License... licenses) {
		this.artifactId = artifactId;
		this.groupId = groupId;
		this.version = version;
		this.licenses = Collections.unmodifiableList(Arrays.asList(licenses));
	}

	@Transient
	public boolean hasLicenseComment() {
		for (License l : licenses) {
			if (StringUtils.isNotBlank(l.getComments())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int compareTo(Dependency o) {
		int res = getGroupId().compareTo(o.getGroupId());
		if (res == 0) {
			res = getArtifactId().compareTo(o.getArtifactId());
			if (res == 0) {
				res = getVersion().compareTo(o.getVersion());
			}
		}
		return res;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getVersion() {
		return version;
	}

	public List<License> getLicenses() {
		return licenses;
	}
}
