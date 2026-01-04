package com.doLink_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // JPA Auditing 활성화 (엔티티의 생성/수정 시간 자동 관리)
public class DoLinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoLinkApplication.class, args);
	}

}
