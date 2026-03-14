dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Kafka (EDA - 주문 완료 이벤트 구독)
    implementation("org.springframework.kafka:spring-kafka")

    // DB
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
