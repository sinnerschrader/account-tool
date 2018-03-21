package com.sinnerschrader.s2b.accounttool.logic.component.ldap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration;
import com.sinnerschrader.s2b.accounttool.config.ldap.LdapManagementConfiguration;
import com.sinnerschrader.s2b.accounttool.logic.DateTimeHelper;
import com.sinnerschrader.s2b.accounttool.logic.component.mail.MailService;
import com.sinnerschrader.s2b.accounttool.logic.entity.Group;
import com.sinnerschrader.s2b.accounttool.logic.entity.User;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.sort;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;


@Service
public class LdapBusinessService implements InitializingBean {

    private final static Logger log = LoggerFactory.getLogger(LdapBusinessService.class);

    private final static String NEAR_FUTURE_EXITING = "leavingUsers";

    private final static String EXITED_ACTIVE_USERS = "unmaintainedUsers";

    private final static String ACTIVE_MAIL_ON_INACTIVE_USER = "unmaintainedMailUsers";

    private Cache<String, List<User>> userCache = null;

    @Autowired
    private LdapService ldapService;

    @Autowired
    private LdapConfiguration ldapConfiguration;

    @Autowired
    private LdapManagementConfiguration managementConfiguration;

    @Autowired
    private MailService mailService;

    @Override
    public void afterPropertiesSet() throws Exception {
        userCache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();
    }

    @Scheduled(cron = "${ldap-management.jobs.updateUnmaintained.cronExpr}")
    protected void updateUnmaintained() {
        if (!managementConfiguration.getJobs().getUpdateUnmaintained().isActive()
            || !managementConfiguration.getJobs().isActive()) {
            return;
        }

        log.debug("Updating the informations about unmaintained accounts");
        LocalDateTime startTime = LocalDateTime.now();
        final int nextWeeks = managementConfiguration.getLeavingUsersInCW();
        List<User> exitedActiveUsers = new LinkedList<>();
        List<User> futureExitingUser = new LinkedList<>();
        List<User> activeMailAccounts = new LinkedList<>();
        try (LDAPConnection connection = ldapConfiguration.createConnection()) {
            connection.bind(managementConfiguration.getUser().getBindDN(),
                managementConfiguration.getUser().getPassword());

            final int userCount = ldapService.getUserCount(connection);
            final int blockSize = 250;
            LocalDate today = LocalDate.now();
            LocalDate nearFuture = today.plusWeeks(nextWeeks);
            for (int i = 0; i < userCount; i = i + blockSize) {
                List<User> users = ldapService.getUsers(connection, i, blockSize);
                if (CollectionUtils.isEmpty(users)) {
                    continue;
                }

                for (User user : users) {
                    LocalDate exitDate = user.getEmployeeExitDate();
                    if (exitDate != null && user.getSzzStatus() == User.State.active) {
                        if (exitDate.isBefore(today)) {
                            exitedActiveUsers.add(user);
                        } else if (exitDate.isBefore(nearFuture)) {
                            futureExitingUser.add(user);
                        }
                    }
                    if (user.getSzzStatus() == User.State.inactive && user.getSzzMailStatus() == User.State.active) {
                        activeMailAccounts.add(user);
                    }

                }
            }
            log.debug("Found {} user who are exited but have active accounts", exitedActiveUsers.size());
            log.debug("Found {} user who are leaving in the next {} weeks", futureExitingUser.size(), nextWeeks);
            log.debug("Found {} active mail addresses on inactive accounts", activeMailAccounts.size());

            sort(exitedActiveUsers);
            sort(futureExitingUser);
            sort(activeMailAccounts);

            userCache.put(EXITED_ACTIVE_USERS, exitedActiveUsers);
            userCache.put(NEAR_FUTURE_EXITING, futureExitingUser);
            userCache.put(ACTIVE_MAIL_ON_INACTIVE_USER, activeMailAccounts);

            log.info("Updated the informations about unmaintained accounts in {}",
                    DateTimeHelper.INSTANCE.getDurationString(startTime, LocalDateTime.now(), ChronoUnit.MILLIS));
        } catch (LDAPException | GeneralSecurityException e) {
            log.error("Could not update unmaintained users", e);
        }
    }

    @Scheduled(cron = "${ldap-management.jobs.notifyAboutUnmaintained.cronExpr}")
    protected void notifyAboutUnmaintained() {
        if (!managementConfiguration.getJobs().getNotifyAboutUnmaintained().isActive()
            || !managementConfiguration.getJobs().isActive()) {
            return;
        }

        if (userCache.size() > 0) {
            final String[] to = managementConfiguration.getNotifyReceipients().toArray(new String[0]);
            mailService.sendNotificationOnUnmaintainedAccounts(to, userCache.asMap());
        }
    }

    @Scheduled(cron = "${ldap-management.jobs.notifyAboutExpiration.cronExpr}")
    public void notifyAboutExpiration() {
        LocalDate threshold = LocalDate.now().plusWeeks(2);
        List<User> expiringAccounts = getLeavingUsers().stream().filter(user -> threshold.isAfter(user.getEmployeeExitDate())).collect(Collectors.toList());
        mailService.sendMailForAccountExpiration(expiringAccounts);
    }

    public List<User> getUnmaintainedMailUsers() {
        return getUsersByCacheType(ACTIVE_MAIL_ON_INACTIVE_USER);
    }

    public List<User> getUnmaintainedExternals() {
        return getUsersByCacheType(EXITED_ACTIVE_USERS);
    }

    public List<User> getLeavingUsers() {
        return getUsersByCacheType(NEAR_FUTURE_EXITING);
    }

    private List<User> getUsersByCacheType(String type) {
        return defaultIfNull(userCache.getIfPresent(type), Collections.emptyList());
    }

    public void addDefaultGroups(User user) {
        List<String> defaultGroups = ldapConfiguration.getPermissions().getDefaultGroups();
        if (defaultGroups == null || defaultGroups.isEmpty()) {
            log.debug("No default groups defined, skipped adding user to default groups");
            return;
        }
        try (LDAPConnection connection = ldapConfiguration.createConnection()) {
            connection.bind(managementConfiguration.getUser().getBindDN(),
                managementConfiguration.getUser().getPassword());

            for (String groupCn : defaultGroups) {
                Group group = ldapService.getGroupByCN(connection, groupCn);
                if (group != null) {
                    ldapService.addUserToGroup(connection, user, group);
                }
            }
            log.debug("Added user to {} default groups", defaultGroups.size());
        } catch (LDAPException | GeneralSecurityException e) {
            log.error("Could not add user to default groups", e);
        }
    }

    public void delDefaulGroups(User user) {
        List<String> defaultGroups = ldapConfiguration.getPermissions().getDefaultGroups();
        if (defaultGroups == null || defaultGroups.isEmpty()) {
            log.debug("No default groups defined, skipped removing user from default groups");
            return;
        }
        try (LDAPConnection connection = ldapConfiguration.createConnection()) {
            connection.bind(managementConfiguration.getUser().getBindDN(),
                managementConfiguration.getUser().getPassword());

            for (String groupCn : defaultGroups) {
                Group group = ldapService.getGroupByCN(connection, groupCn);
                if (group != null) {
                    ldapService.removeUserFromGroup(connection, user, group);
                }
            }
            log.debug("Removed user from {} default groups", defaultGroups.size());
        } catch (LDAPException | GeneralSecurityException e) {
            log.error("Could not remove user from default groups", e);
        }
    }

}
