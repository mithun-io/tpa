package com.tpa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@RequiresDocker
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TpaApplicationTests {

    @Test
    void contextLoads() {
    }

}
