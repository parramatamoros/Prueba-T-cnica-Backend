# ── Stage 1: Build ─────────────────────────────────────────────────────
# Usamos la imagen oficial de Maven con JDK 17 para compilar
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiamos el pom primero para aprovechar el caché de capas de Docker.
# Si solo cambia el código (no las dependencias), esta capa no se reconstruye.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos (omitimos tests en el build de imagen)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────────────────
# Imagen mínima de JRE (no JDK completo) para reducir el tamaño final
FROM eclipse-temurin:17-jre-alpine

# Usuario no-root por seguridad (no ejecutamos como root en producción)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copiamos solo el jar compilado desde el stage de build
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto de la aplicación
EXPOSE 8080

# Configuraciones JVM para contenedores:
#  -XX:+UseContainerSupport: respeta los límites de memoria del contenedor
#  -XX:MaxRAMPercentage=75: usa máximo el 75% de la RAM asignada
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
