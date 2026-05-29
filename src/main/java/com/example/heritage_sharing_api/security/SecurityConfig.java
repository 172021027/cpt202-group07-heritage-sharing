package com.example.heritage_sharing_api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/admin/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resources").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,
                        "/api/resources/archive/*",
                        "/api/resources/offline/*",
                        "/api/resources/restore/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/resources/actions/history").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/api/resources/approved",
                        "/api/resources/search",
                        "/api/resources/filter",
                        "/api/resources/search-and-filter",
                        "/api/resources/list-frontend",
                        "/api/resources/*").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/categories",
                        "/api/tags",
                        "/api/tags/merge").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,
                        "/api/categories/*",
                        "/api/tags/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,
                        "/api/categories/*",
                        "/api/tags/*").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/categories", "/api/categories/*").authenticated()
                .requestMatchers("/api/tags", "/api/tags/*").authenticated()
                .requestMatchers("/api/resources/submit").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/resources/mine/*").authenticated()
                .requestMatchers("/api/resources/**").permitAll()
                .requestMatchers("/*.html").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
