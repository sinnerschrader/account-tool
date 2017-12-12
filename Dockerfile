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
RUN gradle --no-daemon build

ENV ENVIRONMENT=production \
    GIT_BRANCH=some_branch \
    GIT_COMMIT=some_commit

CMD gradle --no-daemon bootRun
