[![Build Status](https://travis-ci.org/sinnerschrader/account-tool.svg?branch=master)](https://travis-ci.org/sinnerschrader/account-tool)
[![Known Vulnerabilities](https://snyk.io/test/github/sinnerschrader/account-tool/badge.svg)](https://snyk.io/test/github/sinnerschrader/account-tool)

# SinnerSchrader Account Tool

![Logo](/src/main/resources/public/static/favicons/mstile-144x144.png)

The SinnerSchrader Account Tool is a user management and self service application for [OpenLDAP](https://www.openldap.org/). It allows you to create and manage users and groups with ease.

![Alt Text](https://media.giphy.com/media/TMiG4GLEFE3wA/giphy.gif)


## Setup and guidelines

### Run the application
* Run `docker-compose up`
* Open [http://localhost:8080](http://localhost:8080) in your browser
* Login with `accadm` and password `testuser`
* Open [http://localhost:1080](http://localhost:1080) for [maildev](http://danfarrelly.nyc/MailDev/)

You can also login with any other user contained in the test data set. The password is always `testuser`. Please refer to `src/main/resources/ldap/data/03-testuser.ldif` for further details.

## LDAP structure

### Base
The LDAP root is `dc=example,dc=org` where all entries are stored. This can be configured in your yml file.
This tool can handle multiple companies, where each company has a sub-tree with it's own `ou=users` entry.
The groups are still stored globally under `ou=groups` to allow a collaboration on the same project.

### Groups
All groups have to be created twice. One with a team admin prefix and one with a team prefix. The idea of it, is that some project admins can administrate the team. The team admin groups are always the administrator groups of the normal customer projectteam Group. The absolute fallback is always the group you defined in `ldap.permissions.admins`, if no other admin group was found.

## Password checks via zxcvbn
We are using the zxcvbn4j library, which is a java port of the original lib. You can find it on GitHub: https://github.com/nulab/zxcvbn4j. In the folder `src/resources/zxcvbn` we store a dictionary that contains the public top 500 list of bad password ideas. You can find it [here](http://www.whatsmypass.com/the-top-500-worst-passwords-of-all-time).

#### Files and content
The test data and schema is stored under `src/main/resources/ldap/*` with two folders inside. The folder `schema` contains the LDAP schema files in an LDIF format which is required to validate the data which is stored in the `data` folder.

##### Schema files
We currently use some of the default and publicly provided schema files (`01-system` to `06-ppolicy`), but we require some extensions. The custom extension is the integration of the Samba schema to handle some Samba shares.
The schema file 08-szz provides a custom extension for handling employees over time.

##### Data files
* `01-company-structure.ldif`
  * Contains all OUs (Organizational Units) which describe the base company structure
* `02-groups.ldif`
  * many groups which are used for customers / project Teams and third party services for example Jira, Git, etc.
* `03-testuser.ldif`
  * A dump of users, with reseted passwords to default values. Every user has the password `testuser` in the embeded LDAP.
  * The reset password is required, to perform several checks about the permissions, because the tool has to handle different views for different permissions.
