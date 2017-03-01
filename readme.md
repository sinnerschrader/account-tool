# SinnerSchrader Account Tool

## Setup

### Prerequisits
* Install Java JDK 1.8.x (Oracle or OpenJDK)
* Install NodeJS 7.x and NPM
* Install a Maven or Use Maven Wrapper inside the Project
  * Check if the wrapper is running `./mvnw --version`
  * Perform the Maven Security Steps (independant which maven you use!) 
    * Google for "Maven Security" it will be described on an Apache Website

### First Run
* Run `mvn clean spring-boot:run` or `./mvnw clean spring-boot:run` 
* Open `localhost:8080` in your Browser
* Login with any user and password `testuser`
  * Some Testusers and the assigned Company you can find `03-testuser.ldif` File inside this Project

## Deployments
The deployment is currently not automatized. This leads to some required manual steps.

### Merge Request
The master Branch is protected an nobody can and schould ever push to it! Every feature has to be integrated over an
Feature Branch (Prefix `feat/`) or a BugFix Branch (Prefix `fix/`) followed by a short and meaningful title.

### Create a release
This Project uses Continue Delivery and Continious Integration. On every accept of an Merge Request into the master,
the Gitlab CI executes the `deploy-to-nexus` Job. The Version Number will be set automatically over Git.
For further Information have a look into `git describe`. The automatic Version are generated based on the latest tag
and the distance on commits to it. 

### Deploy to a environment
The Deployment is performed over the Saltstack. For a Deployment you need two Repositories.
* Formula: szops-saltstack/formula-szops_accounttool
* Configuration: szops-saltstack/customer-customer

The formula Repository contains the information about the Package to deploy and the setup 
instruction like folder, user creation and permissions. This Project currently also contains
a version of the Debian Package which have to deployed. 
The Debian Package schould be removed from Git in the near future.

The Configuration Project contains all relevant informations about the node. Here you can 
find the YAML Configuration File (`pillar / stack / nodes / node1-accounttool1.szz.ham2.szops.net.yaml`) 
for the Account Tool. In this file all relevant informations about the setup,
like SSL Certs, which Packages and Services should be installed (also the Debian Package for the Account Tool).
The current configuration uses the salt:// Protocol to retrieve the Deb File from Saltstack Master, 
but this has to be replaced with a Download from Nexus instead (currently the download has to be done manually).

## LDAP Structure

### Base
The LDAP root is `dc=example,dc=org` where all entries are stored.
The structure was extended due merge of all companies of Sinnerschrader.
Each company has a sub-tree with it's own `ou=users` entry. 
The Groups are still stored globally under `ou=groups`.
 
### Groups
All Group have to be created twice. One with a `s2a` Prefix and one with `s2f` 
Prefix. The s2a Groups are always the Administrator Groups of the normal 
Customer Projectteam Group (prefixed with s2f).
Admin Groups are identified by the s2a Prefix or a `admins` or `administrators` 
in the Name. The absolut fallback is always the `ldap-admins` Group, if no other 
Admin Group was found.

## Development
Run `mvn clean spring-boot:run` on command line. 
The Webserver starts automatically and you can reach 
under http://localhost:8080 the account tool.

For development mode you can use `accadm` which is an admin account. 
Please refer to `src/main/resources/ldap/data/03-testuser.ldif` for Company selection and password.
 
## Password checks via zxcvbn
We are using the zxcvbn4j library, which is a java port of the original lib. 
You can find it on GitHub: https://github.com/nulab/zxcvbn4j
In the folder `src/resources/zxcvbn` we stored two dictionary files. Firts one is our
S2 internal dictionary from s2crack. Ask `@ingben` for an update or further informations.
The second one is a public Top 500 List of bad password ideas. You can find it on http://www.whatsmypass.com/the-top-500-worst-passwords-of-all-time

Please update this lists regulary. Feel free to add more lists; you have to register each in the `application.yaml`

### Local Testenvironment / Data
On local development mode, the application starts an embedded LDAP Service, which is used to make Unit, Integration Test
and testing new features or bugfixes on a defined data.

#### Files and content
The Testdata and Schema is stored under `src/main/resources/ldap/*` with two folders inside. The folder `schema` contains
the LDAP Schema Files in an LDIF format which is required to validate the Data which is stored in `data` folder.

##### Schema Files
We currently used some of the default and public provides schema files (01-system to 06-ppolicy), but we require some
special Schemata more. The first custom extension is the integration of the Samba Schema to handle our Fileservers.
The Schema Files 08-szz and 09-szzRessources are providing custom extension for our Accounts and Resources like Rooms.

Please make a research about LDAP Schema files on the internet, if you need more informations about the first schema files.

##### Data Files
* 01-company-structure.ldif
  * Contains all OUs which describes the base company structure
* 02-groups.ldif
  * many Groups which are used for Customers/Project Teams, Internal Stuff and Access for third party Services like Jira, etc.
* 03-testuser.ldif
  * A dump of users, with reseted passwords to default values. Every User has the password `testuser` in the embeded LDAP.
  * The reset password is required, to perform serveral check about the permissions, because the tool has to handle different views on different permissions.

##### Update Testuser File (03-testuser.ldif)
Please set to all users in this file the same password.
`userPassword: testuser`

Please write it here in plain text. LDIF Import only supports that on runtime, after passwort
change the password will be stored encrypted (if you have enough permissions, otherwise it is hidden), 
but then the login is not working anymore.

Command to create the Export: 
``` bash
$ ldapsearch -x -H ldaps://ldap.daomain.tld -b "dc=doamin,dc=tld" -WD "uid=*UID*,ou=users,dc=domain,dc=tld" "(objectClass=posixAccount)" > ~/users-eport.ldif
```

These Attributes are explicitly excluded for easier set the dummy password.

