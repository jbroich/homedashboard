FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/pom.xml backend/pom.xml

WORKDIR /app/backend
RUN chmod +x mvnw && ./mvnw -DskipTests dependency:go-offline

COPY backend/src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
