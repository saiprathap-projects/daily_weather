# === Stage 1: Maven Build ===
FROM maven:3.9.6 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
# === Stage 2: Tomcat Deploy ===
FROM tomcat:9.0
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=builder /app/target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
