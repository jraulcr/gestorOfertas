# Etapa 1: Construcción (Build)
# Usamos una imagen con Maven y Java 21 para compilar tu código
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Compilamos el proyecto saltándonos los tests para agilizar
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run)
# Usamos una imagen mucho más ligera (JRE) solo para correr la app
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copiamos el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Render asigna un puerto dinámico mediante la variable PORT.
# Le decimos a Spring Boot que use ese puerto.
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT"]