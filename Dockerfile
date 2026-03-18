FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /build/target/contact-manager-pro.jar ./contact-manager-pro.jar

ENTRYPOINT ["java", "-jar", "contact-manager-pro.jar"]
