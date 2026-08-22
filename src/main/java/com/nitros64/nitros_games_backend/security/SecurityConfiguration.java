package com.nitros64.nitros_games_backend.security;

import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nitros64.nitros_games_backend.shared.api.error.ApiProblem;
import com.nitros64.nitros_games_backend.shared.api.error.ApiProblemWriter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiProblemWriter problemWriter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/**").permitAll()
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(
                        (request, response, exception) -> problemWriter.write(
                                response,
                                ApiProblem.create(
                                        HttpStatus.FORBIDDEN,
                                        "Access denied",
                                        "access_denied",
                                        "Administrator privileges are required",
                                        request.getRequestURI()))))
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) ->
                        problemWriter.write(
                                response,
                                ApiProblem.create(
                                        HttpStatus.UNAUTHORIZED,
                                        "Authentication required",
                                        "authentication_required",
                                        "Authentication is required to modify this resource",
                                        request.getRequestURI()))))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(
            SecurityProperties properties,
            PasswordEncoder passwordEncoder) {
        var admin = User.withUsername(properties.getAdminUsername())
                .password(passwordEncoder.encode(properties.getAdminPassword()))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOrigins());
        configuration.setAllowedMethods(
                List.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
