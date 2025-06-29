package com.example.kokkiri.common.config;

import com.example.kokkiri.common.jwt.JwtAuthenticationFilter;
import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.common.jwt.RefreshTokenService;
import com.example.kokkiri.common.oauth.CustomAuthorizationRequestResolver;
import com.example.kokkiri.common.oauth.CustomOAuth2UserService;
import com.example.kokkiri.common.oauth.OAuth2AuthenticationSuccessHandler;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;


    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/members/login",
                                "/images/**",
                                "/api/team/verify",
                                "/api/team/state",
                                "/api/members/signup",
                                "/api/members/refresh",
                                "/api/members/reset",
                                "/api/calendars/**",
                                "/connect/**",
                                "/api/email/**",
                                "/oauth2/**",
                                "/api/members/oauth2/success",
                                "/api/files/**",
                                "/api/problem/**",
                                "/api/submissions/**"
                        ).permitAll()
                        // 관리자만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        //  인증만 되면 접근 가능한 API
                        .requestMatchers("/api/members/me").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization ->
                                authorization
                                        .authorizationRequestResolver(
                                                new CustomAuthorizationRequestResolver(
                                                        clientRegistrationRepository,
                                                        "/oauth2/authorization"
                                                )
                                        )
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)) // 사용자 정보 로드
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, memberRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler(jwtUtil, refreshTokenService, memberRepository, teamRepository,redisTemplate,
                frontendBaseUrl);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:8080",
        "http://192.168.230.207:8080", "http://192.168.230.10:8080"
    ));
//        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));    // 모든 HTTP 메서드 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));    // 모든 헤더값 허용
        configuration.setAllowCredentials(true);                // 자격 증명 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 url의 패턴에 대해 cors 허용 설정

        return source;
    }


}
