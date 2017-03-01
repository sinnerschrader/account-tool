# SinnerSchrader Account Tool

![Logo](/src/main/resources/public/static/favicons/mstile-144x144.png)

## Setup and Guidelines

### Prerequisits
* Install Java JDK 1.8.x (Oracle or OpenJDK)
* Install NodeJS 7.x and NPM
* Install a Maven or Use Maven Wrapper inside the Project
  * Check if the wrapper is running `./mvnw --version`
  * Perform the Maven Security Steps (independant which maven you use!) 
    * Google for "Maven Security" it will be described on an Apache Website

### Run the application 
* Run `mvn clean spring-boot:run` or `./mvnw clean spring-boot:run` 
* Open `localhost:8080` in your Browser
* Login with any user and password `testuser`
  * Please refer to `src/main/resources/ldap/data/03-testuser.ldif` for company selection and password.

### Merge Request
The master Branch is protected an nobody can or schould ever push to it! Every feature has to be integrated over an
Feature Branch (Prefix `feat/`) or a BugFix Branch (Prefix `fix/`) followed by a short and meaningful title.

### Create a release
This Project uses Continue Delivery and Continious Integration. On every accept of a Merge Request into the master,
the Travic CI executes the `deploy` Job. This will create a standalone self containing jar and/or debian package. 
Due to Continous delivery you will get a new version automatically on each merge/pull request.

### Deploy to a environment
The deployment can be easily done via the debian package, which will register an service for starting the
application on every system start. The default on a installation is always a production profile, which is not part
of the jar file itself. The administrator have to configure the environment independant of the used package.

## LDAP Structure

### Base
The LDAP root is `dc=example,dc=org` where all entries are stored. This can be configured in your yml file.
This tool can handle multiple companies, where each company has a sub-tree with it's own `ou=users` entry. 
The Groups are still stored globally under `ou=groups` to allow a collaboration on same projects.
 
### Groups
All Group have to be created twice. One with a team admin Prefix and one with team Prefix. The idea of it, is that some 
Project admins can administrate the Team. The team admin Groups are always the Administrator Groups of the normal Customer 
Projectteam Group. The absolute fallback is always the Group you defined in `ldap.permissions.admins`, 
if no other Admin Group was found.

## Password checks via zxcvbn
We are using the zxcvbn4j library, which is a java port of the original lib. You can find it on 
GitHub: https://github.com/nulab/zxcvbn4jIn 
the folder `src/resources/zxcvbn` we stored two dictionary files. The first one is a custom crack dictionary. 
The second one is a public Top 500 List of bad password ideas. 
You can find it on http://www.whatsmypass.com/the-top-500-worst-passwords-of-all-time

Please update this lists regulary. Feel free to add more lists; you have to register each in the `application.yaml`.
The Lists can be extended, but keep in mind, that a very long list will have performance impacts on the check routines.

### Local Testenvironment / Data
On local development mode, the application starts an embedded LDAP Service, which is used to perform Unit, Integration Test
on a defined data base. This database is also used for the running application on your environment. 
This data was generated, but can be adjusted for specific testings over time.

#### Files and content
The Testdata and Schema is stored under `src/main/resources/ldap/*` with two folders inside. The folder `schema` contains
the LDAP Schema Files in an LDIF format which is required to validate the Data which is stored in `data` folder.

##### Schema Files
We currently used some of the default and public provides schema files (01-system to 06-ppolicy), but we require some
extensions. The custom extension is the integration of the Samba Schema to handle some Samba Shares.
The Schema File 08-szz are providing custom extension for handling employees over time.

Please make a research about LDAP Schema files on the internet, if you need more informations about the first schema files.

##### Data Files
* 01-company-structure.ldif
  * Contains all OUs which describes the base company structure
* 02-groups.ldif
  * many Groups which are used for Customers/Project Teams and third party Services for example Jira, Git, etc.
* 03-testuser.ldif
  * A dump of users, with reseted passwords to default values. Every User has the password `testuser` in the embeded LDAP.
  * The reset password is required, to perform serveral check about the permissions, because the tool has to handle different views on different permissions.

##### Update Testuser File (03-testuser.ldif)
Please set to all users in this file the same password. `userPassword: testuser`
