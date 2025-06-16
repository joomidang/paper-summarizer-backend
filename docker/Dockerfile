FROM openjdk:17-slim

ARG SPRINGBOOT_APP_PROFILE
ENV PROFILE=${SPRINGBOOT_APP_PROFILE:-dev}

COPY build/libs/paper-summary.jar app.jar
COPY /etc/ssl/certs/keystore.p12 /etc/ssl/certs/keystore.p12

ENTRYPOINT ["sh", "-c", "exec java -jar -Dspring.profiles.active=$PROFILE app.jar"]