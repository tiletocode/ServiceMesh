dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client") {
        // Spring Cloud Gateway는 WebFlux(Netty) 기반이므로 Eureka가 전이 의존하는 MVC 웹 제거
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
    }
}
