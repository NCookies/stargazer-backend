package xyz.ncookie.stargazer.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Stargazer API")
				.description("별 관측 조건 분석 및 관측지 추천 서비스 API 문서")
				.version("v1.0.0")
				.contact(new Contact()
					.name("Stargazer Team")
					.email("support@stargazer.com"))
				.license(new License()
					.name("Apache 2.0")
					.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
			.servers(List.of(
				new Server().url("http://localhost:8080").description("로컬 개발 서버"),
				new Server().url("https://api.byeolbolil.xyz").description("프로덕션 서버")
			))
			.components(new Components()
				.addSecuritySchemes("bearer-jwt", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("Authorization")))
			.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}
}
