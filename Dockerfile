FROM maven:3.5-jdk-8-alpine

RUN apk add --no-cache git nodejs nodejs-npm

RUN echo $'\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\n\
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n\
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0\n\
                      https://maven.apache.org/xsd/settings-1.0.0.xsd">\n\
  <localRepository>/usr/share/maven/repo</localRepository>\n\
</settings>'\
>> /usr/share/maven/ref/settings.xml

WORKDIR /app

ADD pom.xml /app/
RUN mvn-entrypoint.sh mvn dependency:go-offline

ADD package.json /app/
RUN npm install

ADD . /app
RUN npm run build \
 && mvn-entrypoint.sh mvn package

ENV ENVIRONMENT=production

CMD mvn spring-boot:run -Drun.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
