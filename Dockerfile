FROM amazoncorretto:17
WORKDIR /app

COPY build/libs/*SNAPSHOT.jar app.jar

# prod 프로필 활성화
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "app.jar"]