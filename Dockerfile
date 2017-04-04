FROM maven:3.3-jdk-8-alpine

RUN apk add --no-cache git nodejs

ADD . /app
WORKDIR /app

CMD mvn install spring-boot:run
