FROM karluto/jdk21-apline3.18:latest
WORKDIR /app
COPY target/homeoffice-2.0.0.jar .
COPY src/main/resources ./src/main/resources
EXPOSE 8077
CMD  java -jar homeoffice-2.0.0.jar