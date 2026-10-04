package com.jp.elearningjp.config;

import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.repository.user.RoleRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_EMAIL = "admin@gmail.com";


    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("Initializing application.....");
        return args -> {
            if (userRepository.findUserByEmail(ADMIN_USER_EMAIL).isEmpty()) {

                log.info("Saving roles...");
                Role teacher = roleRepository.save(Role.builder()
                        .name(PredefinedRole.TEACHER_ROLE)
                        .description("teacher role")
                        .build());
                log.info("Saved role: {}", teacher.getName());
                roleRepository.save(Role.builder()
                        .name(PredefinedRole.USER_ROLE)
                        .description("user role")
                        .build());
                Role adminRole = roleRepository.save(Role.builder()
                        .name(PredefinedRole.ADMIN_ROLE)
                        .description("Admin role")
                        .build());

                var roles = new HashSet<Role>();
                roles.add(adminRole);

                User user = User.builder()
                        .fullName("System Admin")
                        .email(ADMIN_USER_EMAIL)
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(roles)
                        .build();

                userRepository.save(user);


                log.warn("admin user has been created with default password: admin, please change it");
            }
            log.info("Application initialization completed .....");
        };
    }
}