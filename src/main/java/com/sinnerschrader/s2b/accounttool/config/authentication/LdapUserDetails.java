package com.sinnerschrader.s2b.accounttool.config.authentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LdapUserDetails implements UserDetails, Serializable {

	private final List<GrantedAuthority> grantedAuthorities;

	private final String dn;

	private final String displayName;

	private final String uid;

	private final String company;

	private final boolean expired;

	private final boolean enabled;

	private String password;

	public LdapUserDetails(
			String dn,
			String uid,
			String displayName,
			String password,
			String company,
			List<GrantedAuthority> grantedAuthorities,
			boolean expired,
			boolean enabled) {
		this.dn = dn;
		this.uid = uid;
		this.displayName = displayName;
		this.password = password;
		this.company = company;
		this.expired = expired;
		this.enabled = enabled;
		this.grantedAuthorities = new ArrayList<>();
		if (grantedAuthorities != null) {
			this.grantedAuthorities.addAll(grantedAuthorities);
		}
	}

	public String getUid() {
		return uid;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getCompany() {
		return company;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDn() {
		return dn;
	}

	@Override
	public String getUsername() {
		return uid;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.unmodifiableList(grantedAuthorities);
	}

	@Override
	public boolean isAccountNonExpired() {
		return !expired;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
