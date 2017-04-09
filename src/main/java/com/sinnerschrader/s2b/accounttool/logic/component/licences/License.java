package com.sinnerschrader.s2b.accounttool.logic.component.licences;

/**
 * Created by vikgru on 19/01/2017.
 */
public class License {

	private final String name;

	private final String url;

	private final String distribution;

	private final String comments;

	License(String name, String url, String distribution, String comments) {
		this.name = name;
		this.url = url;
		this.distribution = distribution;
		this.comments = comments;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getDistribution() {
		return distribution;
	}

	public String getComments() {
		return comments;
	}
}
