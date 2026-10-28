package com.kisandaily.auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
@Configuration
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http,AuthService auth,ObjectMapper json,
            @Value("${app.auth-requests-per-minute}") int requests) throws Exception {
        // Only explicit Authorization bearer headers are accepted. There are no login cookies.
        return http.csrf(AbstractHttpConfigurer::disable).cors(c->{}).formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/health").permitAll().anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->writeError(json,res,401,"UNAUTHORIZED","Session समाप्त है। फिर login करें"))
                .accessDeniedHandler((req,res,ex)->writeError(json,res,403,"FORBIDDEN","इस कार्रवाई की अनुमति नहीं है")))
            .addFilterBefore(new BearerFilter(auth,json,requests),UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean CorsConfigurationSource cors(@Value("${app.cors-origins}") String origins) {
        CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type"));c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",c);return source;
    }
    static void writeError(ObjectMapper json,HttpServletResponse res,int status,String code,String message) throws IOException {
        res.setStatus(status);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getWriter(),new AuthDtos.Error(code,message,Map.of()));
    }
    static class BearerFilter extends OncePerRequestFilter {
        private final AuthService auth; private final ObjectMapper json; private final int limit;
        private final Map<String,Bucket> buckets=new HashMap<>();
        record Bucket(long minute,int count) {}
        BearerFilter(AuthService auth,ObjectMapper json,int limit) {this.auth=auth;this.json=json;this.limit=Math.max(1,limit);}
        private synchronized boolean allowed(String ip) {
            long minute=System.currentTimeMillis()/60000;
            buckets.entrySet().removeIf(e->e.getValue().minute()!=minute);
            Bucket b=buckets.get(ip);
            if(b==null && buckets.size()>=10000) return false;
            if(b!=null && b.count()>=limit) return false;
            buckets.put(ip,new Bucket(minute,b==null?1:b.count()+1));return true;
        }
        @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
            if(req.getMethod().equals("POST") && Set.of("/api/auth/login","/api/auth/register").contains(req.getRequestURI()) && !allowed(req.getRemoteAddr())) {
                res.setHeader("Retry-After","60");writeError(json,res,429,"RATE_LIMITED","बहुत कोशिशें हो गई हैं। एक मिनट बाद फिर कोशिश करें");return;
            }
            String header=req.getHeader(HttpHeaders.AUTHORIZATION);
            if(header!=null && header.startsWith("Bearer ")) {
                auth.authenticate(header.substring(7)).ifPresent(p->SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of())));
            }
            chain.doFilter(req,res);
        }
    }
}
