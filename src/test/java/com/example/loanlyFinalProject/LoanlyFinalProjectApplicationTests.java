package com.example.loanlyFinalProject;

import com.example.loanlyFinalProject.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class LoanlyFinalProjectApplicationTests {

  @Test
  void contextLoads() {}
}
