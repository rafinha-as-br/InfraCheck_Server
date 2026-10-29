# Imagem da API InfraCheck (build em duas etapas).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/infracheck-server-*.jar app.jar
# Fotos e assinaturas (dados sensíveis): montar um volume persistente aqui.
ENV INFRACHECK_STORAGE_PATH=/data/storage
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
