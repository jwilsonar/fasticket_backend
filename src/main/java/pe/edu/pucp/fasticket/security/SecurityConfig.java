package pe.edu.pucp.fasticket.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/**
 * Configuración de seguridad de Spring Security con JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Value("${app.cors.dev-enabled:false}")
    private boolean corsDevEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {}) // CORS se configura desde application.properties o bean corsConfigurationSource
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/eventos/publicos/**",
                                "/swagger-ui/**",
                                "/api/v1/public/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers("/api/v1/geografia/**").permitAll()
                        //Enpoint extra para tema de OAUth
                        .requestMatchers("/", "/public/**").permitAll()
                        // Endpoints de solo lectura para clientes
                        .requestMatchers(HttpMethod.GET, "/api/v1/eventos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locales/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/zonas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tipos-ticket/**").permitAll()
                        
                        // Endpoints de administración
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/eventos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/eventos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/eventos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/zonas/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/zonas/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/zonas/**").hasRole("ADMINISTRADOR")
                        
                        // Endpoints de compras (requiere autenticación)
                        .requestMatchers("/api/v1/compras/**").authenticated()
                        .requestMatchers("/api/v1/carrito/**").authenticated()
                        .requestMatchers("/api/v1/ordenes/**").authenticated()
                        
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")     // Opcional: tu página de login
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                );

        return http.build();
    }

    /**
     * Configuración de CORS. En desarrollo, si app.cors.dev-enabled=true,
     * se permite cualquier origen, método y header.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (corsDevEnabled) {
            configuration.addAllowedOriginPattern("*");
            configuration.addAllowedHeader("*");
            configuration.addAllowedMethod("*");
            configuration.setAllowCredentials(true);
        } else {
            // Respeta la configuración por properties (por defecto Spring la lee de spring.web.cors.*)
            configuration.addAllowedOrigin("http://localhost:4200");
            configuration.addAllowedHeader("*");
            configuration.addAllowedMethod("*");
            configuration.setAllowCredentials(true);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

