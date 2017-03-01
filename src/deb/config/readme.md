[comment]: ###############################################################################
# S2 Account Tool
[comment]: ###############################################################################

You installed the Debian Package of the Account Tool. In this
Directory Structure you can find multiple directories.

## Structure:

```
/usr/share/account-tool/
+-- bin
|   +-- account-tool: start script; this is used by systemd!
+-- conf
|   +-- overrides.yaml: Contains an overrides config for your environment
|   +-- example.yaml: a Example of the overrides.yaml where you can copy config from
+-- lib: 
|   +-- .jar: Self containing Jar with full application is stored here.
+-- oauth_template
    +-- .html: Signin and error HTML Template for a Bitly OAuth Proxy
```

# systemd - Service registration
After installation the Account Tool is registered as a SystemD Service and
will start and stop during automatically. For this we registered a service.conf
file into Systemd.

# Defaults
We store under /etc/defaults/accounttool a default file. For testing
purpose you can switch the SPRING_ACTIVE_PROFILES from production to
development. After that, an embedded LDAP is used.

# Development Mode

Credentials: all User have "testuser" as their password.
Admin: accadm (Company 1)
User Managenemt: usrmng (Company 1)
Testuser: musmax (Company 2)
