package com.vcube.stockwise.config;

import com.vcube.stockwise.service.AuthService;
import com.vcube.stockwise.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain api(HttpSecurity http,AuthService auth)throws Exception{
        return http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults())
          .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a->a.requestMatchers("/api/auth/register","/api/auth/registration-otp","/api/auth/registration-otp/verify","/api/auth/login","/api/auth/verify-email","/api/auth/verify-email-otp","/api/auth/resend-verification","/api/auth/forgot-password","/api/auth/reset-password","/error").permitAll().anyRequest().authenticated())
          .exceptionHandling(e->e.authenticationEntryPoint((request,response,exception)->response.sendError(401)))
          .addFilterBefore(new BearerTokenFilter(auth),UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean UserDetailsService databaseUsers(AppUserRepository users){return email->users.findByEmailIgnoreCase(email).map(user->User.withUsername(user.getEmail()).password(user.getPasswordHash()).authorities("ROLE_"+user.getRole()).build()).orElseThrow(()->new UsernameNotFoundException("User not found"));}
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin-patterns}") String allowedOriginPatterns){
        var c=new CorsConfiguration();c.setAllowedOriginPatterns(java.util.Arrays.stream(allowedOriginPatterns.split(",")).map(String::trim).filter(value->!value.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type"));
        var s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
    }
}
