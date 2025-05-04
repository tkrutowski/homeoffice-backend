FROM karluto/jdk21-apline3.18:latest

# Install tzdata for timezone management
RUN apk add --no-cache tzdata
# Set the timezone to your desired one, e.g., Europe/Warsaw
ENV TZ=Europe/Warsaw

# Możesz ustawić domyślną wartość zmiennej
ENV DEBUG=false

ARG APP_VERSION=latest
ENV APP_VERSION=${APP_VERSION}

# Set default cron value if not provided
ENV SCHEDULER_CRON="0 0 8 * * FRI"

WORKDIR /app
#COPY target/homeoffice-3.6.0.jar .
COPY target/homeoffice-${APP_VERSION}.jar app.jar
COPY src/main/resources ./src/main/resources
EXPOSE 8077
#CMD  java -jar homeoffice-3.6.0.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]