# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/pom.xml backend/pom.xml

WORKDIR /app/backend
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# now copy the actual source and build
COPY backend/src backend/src
RUN ./mvnw -DskipTests package

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# most spring boot jars end up in target/*.jar
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]