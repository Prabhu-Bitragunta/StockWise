package com.vcube.stockwise.config;

import com.vcube.stockwise.domain.AppUser;
import com.vcube.stockwise.repo.AppUserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Configuration
public class BootstrapAdminInitializer {
    @Bean
    @Order(10)
    ApplicationRunner initializeBootstrapAdmin(AppUserRepository users, PasswordEncoder passwords) {
        return args -> {
            String email = System.getenv("BOOTSTRAP_ADMIN_EMAIL");
            String password = System.getenv("BOOTSTRAP_ADMIN_PASSWORD");
            if (email == null || email.isBlank() || password == null || password.length() < 12) {
                return;
            }

            AppUser admin = users.findByEmailIgnoreCase(email.trim())
                    .orElseGet(() -> users.save(new AppUser(
                            email.trim().toLowerCase(Locale.ROOT),
                            "Workspace Admin",
                            passwords.encode(password))));
            admin.setRole("ADMIN");
            users.save(admin);
        };
    }
}
