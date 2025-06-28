package com.example.kokkiri.common.jwt;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        // [1] Authorization 헤더 로깅 (DEBUG)
        if (log.isDebugEnabled()) {
            String shortToken = token == null ? null : (token.length() > 20 ? token.substring(0, 15) + "..." : token);
            log.debug("[JwtAuth] Authorization 헤더(토큰 일부): {}", shortToken);
        }

        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token);
                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + member.getRole().name());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(member, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // [2] 토큰 인증 성공 (INFO)
                    log.info("[JwtAuth] ✅ JWT 인증 성공 - email: {}, role: {}", email, member.getRole());
                } else {
                    // [3] 시그니처·구조 불일치 (WARN)
                    log.warn("[JwtAuth] ❌ 유효하지 않은 JWT (구조/서명 등 문제)");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token");
                    return;
                }
            } catch (ExpiredJwtException e) {
                log.warn("[JwtAuth] ❌ 만료된 JWT ({}): {}", shortToken(e.getMessage()), e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expired");
                return;
            } catch (MalformedJwtException e) {
                log.warn("[JwtAuth] ❌ 변조된 JWT: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Malformed token");
                return;
            } catch (io.jsonwebtoken.SignatureException e) {
                log.warn("[JwtAuth] ❌ JWT 서명 불일치: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid signature");
                return;
            } catch (IllegalArgumentException | io.jsonwebtoken.UnsupportedJwtException e) {
                log.warn("[JwtAuth] ❌ JWT 파싱 실패: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token error");
                return;
            } catch (UsernameNotFoundException e) {
                log.warn("[JwtAuth] ❌ 사용자 없음: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("User not found");
                return;
            } catch (Exception e) {
                log.error("[JwtAuth] ❌ 인증 처리 중 예외: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Authentication error");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Authorization: Bearer ... 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    // 토큰 일부만 반환 (로그에서 전부 찍지 않기)
    private String shortToken(String token) {
        if (token == null) return null;
        if (token.length() < 18) return token;
        return token.substring(0, 10) + "..." + token.substring(token.length() - 8);
    }
}
