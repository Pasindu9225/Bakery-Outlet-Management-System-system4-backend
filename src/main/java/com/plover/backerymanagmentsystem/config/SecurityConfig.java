package com.plover.backerymanagmentsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; //  Important for matching specific methods
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.extern.slf4j.Slf4j;
import com.plover.backerymanagmentsystem.core.login.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("Security configuration initialized");
    }

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Use JWT authentication instead");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for testing
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Use explicitly defined CORS source
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )
                .authorizeHttpRequests(bmsauth -> bmsauth
                        .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/bmsauth/**").permitAll() 
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/bom/all", "/api/v1/admin/product/all", "/api/v1/test/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/manager/v1/discounts/**").hasAnyRole("1", "2", "8", "10", "15", "ADMIN", "MANAGER", "POS", "FINANCE")
                        .requestMatchers("/api/manager/outlets/**", "/api/manager/production-centers/**", "/api/manager/transfer-notes/**").hasAnyRole("1", "2", "10", "12", "13", "14", "15", "ADMIN", "MANAGER", "BAKERY", "KITCHEN", "FINANCE", "MPC")
                        .requestMatchers("/api/manager/**").hasAnyRole("1", "2", "10", "12", "13", "14", "15", "ADMIN", "MANAGER", "FINANCE", "BAKERY", "KITCHEN", "MPC")
                        .requestMatchers("/api/storekeeper/**", "/STK/**", "/api/v1/storekeeper/**").hasAnyRole("1", "9", "10", "12", "13", "14", "15", "ADMIN", "STOREKEEPER", "MANAGER", "BAKERY", "KITCHEN", "MPC", "FINANCE")
                        .requestMatchers("/api/pos/**").hasAnyRole("1", "8", "9", "10", "14", "15", "ADMIN", "POS", "STOREKEEPER", "MANAGER", "MPC", "FINANCE")
                        .requestMatchers("/api/admin/**", "/api/v1/admin/**", "/ADMIN/v1/**").hasAnyRole("1", "14", "15", "ADMIN", "MPC", "FINANCE")
                        .requestMatchers("/api/v1/worker/**").hasAnyRole("1", "12", "13", "14", "ADMIN", "BAKERY", "KITCHEN", "MPC")
                        .requestMatchers("/api/v1/finance/**").hasAnyRole("1", "15", "20", "ADMIN", "FINANCE", "MIS")
                        .requestMatchers("/api/v1/mis/**").hasAnyRole("1", "20", "ADMIN", "MIS")
                        .requestMatchers("/api/v1/notifications/**").hasAnyRole("1", "2", "8", "9", "10", "11", "12", "13", "14", "15", "20", "ADMIN", "POS", "STOREKEEPER", "MANAGER", "BAKERY", "KITCHEN", "FINANCE", "MIS")

                        .anyRequest().authenticated())
                .formLogin(form -> form.disable()) // No login page
                .httpBasic(httpBasic -> httpBasic.disable()) // No basic auth
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*"); // Allow all origins for testing
        configuration.addAllowedMethod("*"); // Allow all HTTP methods
        configuration.addAllowedHeader("*"); // Allow all headers
        configuration.setAllowCredentials(false); // Don't allow credentials for wildcard origin

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
