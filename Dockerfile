FROM eclipse-temurin:17

WORKDIR /app
COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

CMD ["./mvnw", "exec:java"]
