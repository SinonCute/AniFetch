package live.karyl.anifetch.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf().disable()
				.authorizeHttpRequests()
				.requestMatchers(HttpMethod.OPTIONS, "/graphql").permitAll()
				.requestMatchers(HttpMethod.POST, "/graphql").permitAll()
				.requestMatchers( HttpMethod.GET,"/v2/api/**").permitAll()
				.anyRequest().authenticated();
		http.headers().cacheControl().disable();
		return http.build();
	}
}
