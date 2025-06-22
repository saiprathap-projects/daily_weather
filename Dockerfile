FROM maven:3.9.6
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

