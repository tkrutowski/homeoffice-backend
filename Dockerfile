FROM karluto/jdk21-apline3.18:latest

# Install tzdata for timezone management
RUN apk add --no-cache tzdata
# Set the timezone to your desired one, e.g., Europe/Warsaw
ENV TZ=Europe/Warsaw

# Możesz ustawić domyślną wartość zmiennej
ENV DEBUG=false

# Set default cron value if not provided
ENV SCHEDULER_CRON="0 0 8 * * FRI"

WORKDIR /app
COPY target/homeoffice-2.6.0.jar .
COPY src/main/resources ./src/main/resources
EXPOSE 8077
CMD  java -jar homeoffice-2.6.0.jar