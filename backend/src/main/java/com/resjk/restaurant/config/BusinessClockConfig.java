package com.resjk.restaurant.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessClockConfig {
  @Bean
  public Clock businessClock() { return Clock.system(ZoneId.of("Asia/Shanghai")); }
}
