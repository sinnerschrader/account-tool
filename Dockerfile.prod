FROM openjdk:jre-alpine

ADD docker/entrypoint.sh \
    build/libs/account-tool-*.jar \
    /app/

WORKDIR /app

ENV JAVA_OPTS="-Xmx256m -Xss256k"
ENV SPRING_PROFILES_ACTIVE=overrides 

ENTRYPOINT [ "/app/entrypoint.sh" ]
CMD java -jar account-tool-*.jar
