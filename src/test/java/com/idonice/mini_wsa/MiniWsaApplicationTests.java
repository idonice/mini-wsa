package com.idonice.mini_wsa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MiniWsaApplicationTests {

	@Test
	void contextLoads() {
	}

}
