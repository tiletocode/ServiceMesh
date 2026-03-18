dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    // API Docs 통합 (각 서비스의 /v3/api-docs 를 하나의 Swagger UI로 집계)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.6")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client") {
        // Spring Cloud Gateway는 WebFlux(Netty) 기반이므로 Eureka가 전이 의존하는 MVC 웹 제거
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
    }
}
