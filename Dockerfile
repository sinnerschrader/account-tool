FROM gradle:jdk8-alpine

USER root
RUN apk add --no-cache git nodejs nodejs-npm

WORKDIR /app

ENV GRADLE_USER_HOME=/gradle
ADD build.gradle /app/
RUN gradle --no-daemon --refresh-dependencies

ADD package.json /app/
RUN npm install

ADD . /app
RUN npm run build \
 && gradle build

ENV ENVIRONMENT=production

CMD gradle --no-daemon bootRun -Drun.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
