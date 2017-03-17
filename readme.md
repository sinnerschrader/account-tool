[![Build Status](https://travis-ci.org/sinnerschrader/account-tool.svg?branch=master)](https://travis-ci.org/sinnerschrader/account-tool)
[![Known Vulnerabilities](https://snyk.io/test/github/sinnerschrader/account-tool/badge.svg)](https://snyk.io/test/github/sinnerschrader/account-tool)

# SinnerSchrader Account Tool

![Logo](/src/main/resources/public/static/favicons/mstile-144x144.png)

## Setup and guidelines

### Prerequisits
* Install Java JDK 1.8.x (Oracle or OpenJDK)
* Install NodeJS 7.x and NPM
* Install a Maven or Use Maven Wrapper inside the Project

### Run the application
* Run `mvn clean spring-boot:run` or `./mvnw clean spring-boot:run`
* Open `localhost:8080` in your browser
* Login with any user and password `testuser`
  * Please refer to `src/main/resources/ldap/data/03-testuser.ldif` for company selection and password.

### Pull requests
The master branch is protected an nobody can or should ever push to it! Every feature has to be integrated through a
feature branch (Prefix `feat/`) or a bugfix branch (Prefix `fix/`) followed by a short and meaningful title.

### Create a release
This project uses continuous delivery and continuous integration. On every merge of a pull request into the master,
the travic CI executes the `deploy` job. This will create a standalone, self containing jar and/or debian package.
Due to continuous delivery you will get a new version automatically on each merge/pull request.

### Deploy to a environment
The deployment can be easily done via the debian package, which will register a service for starting the
application on every system start. The default on a installation is always a production profile, which is not part
of the jar file itself. The administrator has to configure the environment independent of the used package.

## LDAP structure

### Base
The LDAP root is `dc=example,dc=org` where all entries are stored. This can be configured in your yml file.
This tool can handle multiple companies, where each company has a sub-tree with it's own `ou=users` entry.
The groups are still stored globally under `ou=groups` to allow a collaboration on the same project.

### Groups
All groups have to be created twice. One with a team admin prefix and one with a team prefix. The idea of it, is that some project admins can administrate the team. The team admin groups are always the administrator groups of the normal customer projectteam Group. The absolute fallback is always the group you defined in `ldap.permissions.admins`, if no other admin group was found.

## Password checks via zxcvbn
We are using the zxcvbn4j library, which is a java port of the original lib. You can find it on
GitHub: https://github.com/nulab/zxcvbn4j
In the folder `src/resources/zxcvbn` we stored two dictionary files. The first one is a custom crack dictionary.
The second one is a public top 500 list of bad password ideas.
You can find it on http://www.whatsmypass.com/the-top-500-worst-passwords-of-all-time

Please update these lists regulary. Feel free to add more lists; you have to register each in the `application.yaml`.
The lists can be extended, but keep in mind, that a very long list will have performance impacts on the check routines.

### Local Test Environment / Data
In local development mode, the application starts an embedded LDAP service, which is used to perform Unit and Integration Tests on a defined data base. This database is also used for the running application on your environment.
This data was generated, but can be adjusted for specific testings over time.

#### Files and content
The test data and schema is stored under `src/main/resources/ldap/*` with two folders inside. The folder `schema` contains the LDAP schema files in an LDIF format which is required to validate the data which is stored in the `data` folder.

##### Schema files
We currently use some of the default and publicly provided schema files (01-system to 06-ppolicy), but we require some extensions. The custom extension is the integration of the Samba schema to handle some Samba shares.
The schema file 08-szz provides a custom extension for handling employees over time.

Please research LDAP schema files on the internet, if you need more informations about the first schema files.

##### Data files
* 01-company-structure.ldif
  * Contains all OUs (Organizational Units) which describe the base company structure
* 02-groups.ldif
  * many groups which are used for customers / project Teams and third party services for example Jira, Git, etc.
* 03-testuser.ldif
  * A dump of users, with reseted passwords to default values. Every user has the password `testuser` in the embeded LDAP.
  * The reset password is required, to perform serveral checks about the permissions, because the tool has to handle different views for different permissions.
