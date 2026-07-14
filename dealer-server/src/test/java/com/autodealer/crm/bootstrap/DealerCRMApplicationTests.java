package com.autodealer.crm.bootstrap;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DealerCRMApplication.class, properties = {
    "spring.data.redis.port=63790",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=never",
    "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl"
})
@ActiveProfiles("test")
class DealerCRMApplicationTests {

    @Test
    void contextLoads() {
    }

}
