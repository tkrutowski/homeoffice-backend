FROM eclipse-temurin:25-jre-alpine

# Install tzdata for timezone management and fonts for PDF generation
RUN apk add --no-cache tzdata fontconfig ttf-dejavu ttf-liberation
# Set the timezone to your desired one, e.g., Europe/Warsaw
ENV TZ=Europe/Warsaw

# Możesz ustawić domyślną wartość zmiennej
ENV DEBUG=false

ARG APP_VERSION=latest
ENV APP_VERSION=${APP_VERSION}

# Set default cron value if not provided
ENV SCHEDULER_CRON="0 0 8 * * FRI"

WORKDIR /app
# Wildcard zamiast dopasowywania po APP_VERSION - w target/ jest zawsze dokladnie jeden
# spakowany jar (Maven nazywa go wg <version> z pom.xml), wiec nie trzeba tej wersji
# przekazywac z zewnatrz zeby zbudowac obraz lokalnie (docker-compose). APP_VERSION ponizej
# to tylko metadana (ENV w kontenerze / tag obrazu w CI), nieuzywana nigdzie w kodzie appki.
COPY target/homeoffice-*.jar app.jar
COPY src/main/resources ./src/main/resources
EXPOSE 8077
#CMD  java -jar homeoffice-3.6.0.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]