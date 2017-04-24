package com.sinnerschrader.s2b.accounttool.config.ldap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.beans.Transient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class LdapBaseConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(LdapBaseConfig.class);

    private String host;

    private int port;

    private boolean ssl;

    private String dc;

    private String baseDN;

    private String groupDN;

    private List<String> userDN;

    private transient Map<String, String> userDnMap;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getDc() {
        return dc;
    }

    public void setDc(String dc) {
        this.dc = dc;
    }

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String getGroupDN() {
        return groupDN;
    }

    public void setGroupDN(String groupDN) {
        this.groupDN = groupDN;
    }

    public List<String> getUserDN() {
        return userDN;
    }

    public void setUserDN(List<String> userDN) {
        this.userDN = userDN;
    }

    @Transient
    public String getUserDnByCompany(String companyKey) {
        return userDnMap.get(companyKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final String rawDataSeparator = ":";
        final int keyPadding = 5;
        userDnMap = new LinkedHashMap<>();
        for (String rawCompany : userDN) {
            String[] keyAndValue = StringUtils.split(rawCompany, rawDataSeparator);
            if (keyAndValue.length == 2) {
                userDnMap.put(keyAndValue[0], keyAndValue[1]);
                log.debug("Loaded UserDN: [{}] : {}", StringUtils.leftPad(keyAndValue[0], keyPadding), keyAndValue[1]);
            } else {
                throw new IllegalStateException("Could not parse company configuration");
            }
        }
    }

}
