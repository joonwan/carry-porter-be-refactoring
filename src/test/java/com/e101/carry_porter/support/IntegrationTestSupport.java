package com.e101.carry_porter.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
public abstract class IntegrationTestSupport {

   protected static final MySQLContainer<?> MYSQL_CONTAINER;

   static {
       MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
               .withDatabaseName("carry_porter_test")
               .withUsername("root")
               .withPassword("1234");

       MYSQL_CONTAINER.start();
   }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
    }
}
