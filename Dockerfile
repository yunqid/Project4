# CMU 95-702 Project 4 — Tomcat 9 + javax.servlet (matches course Labs / IntelliJ Tomcat 9)
# Build includes Maven package step so Codespaces need not commit ROOT.war.
# Optional: pass Atlas URI at runtime, e.g.
#   docker run -p 8080:8080 -e MONGODB_URI='mongodb+srv://...' <image>
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM tomcat:9.0-jdk17-temurin-jammy
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/
