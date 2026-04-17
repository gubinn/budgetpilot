FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/budgetpilot-*.jar app.jar
ENV TZ=Asia/Shanghai
EXPOSE 6060
ENTRYPOINT ["java", "-jar", "app.jar"]
