FROM gradle:jdk8-alpine

USER root
RUN apk add --no-cache git

RUN cd / \
 && wget https://github.com/glowroot/glowroot/releases/download/v0.10.12/glowroot-0.10.12-dist.zip  \
 && unzip glowroot-*.zip \
 && rm glowroot-*.zip \
 && echo '{"web": {"bindAddress": "0.0.0.0"}}' > /glowroot/admin.json

WORKDIR /app

ENV GRADLE_USER_HOME=/gradle
ADD build.gradle /app/
RUN gradle --no-daemon --refresh-dependencies

ADD . /app
RUN gradle --no-daemon build

ENV ENVIRONMENT=production \
    GIT_BRANCH=some_branch \
    GIT_COMMIT=some_commit

CMD gradle --no-daemon bootRun
