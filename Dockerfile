# java 21 jdk 가 포함된 이미지를 베이스로 사용
FROM eclipse-temurin:21-jdk

# 컨테이너 내부 작업 디렉토리를 /app 으로 설정
WORKDIR /app

# 컨테이너 내부에서도 ./gradlew 사용할 수 있도록 Gradle Wrapper file 복사
COPY gradlew gradlew

# window 용 gradle wrapper 복사
COPY gradlew.bat gradlew.bat

# gradle project 설정 파일 복사
COPY settings.gradle settings.gradle

# 의존성과 빌드 설정이 들어있는 핵심 gradle 파일 복사
# spring boot, jpa, security, mqtt 같은 의존성을 이 파일을 통해 가져옴
COPY build.gradle build.gradle

# gradle wrapper 동작에 필요한 디렉토리 복사
COPY gradle gradle

# 실제 application source code 복사
COPY src src

# gradlew 실행 권한 추가
RUN chmod +x gradlew

# spring boot 용 jar file 생성. 결과물은 build/libs/ 아래에 만들어짐.
# --no-daemon 은 docker build  환경에서 gradle demon 을 띄우지 않도록 해서 좀 더 단순하게 실행
RUN ./gradlew bootJar --no-daemon

# build 된 파일을 /app/app.jar 에 복사
RUN cp build/libs/*.jar app.jar

# 이 container 가 8080 포트를 사용하는 application 이라는 걸 명시
# 실제 외부 port 연결은 dockcer-compose 가 결정
EXPOSE 8080

# container 가 시작되면 실행할 기본 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
