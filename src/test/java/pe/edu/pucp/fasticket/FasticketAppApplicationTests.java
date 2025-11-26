package pe.edu.pucp.fasticket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import pe.edu.pucp.fasticket.config.TestConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class FasticketAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
