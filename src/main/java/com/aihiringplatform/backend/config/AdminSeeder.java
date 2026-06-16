package com.aihiringplatform.backend.config;

import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.entity.UserStatus;
import com.aihiringplatform.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByRole(Role.ADMIN).isEmpty()) {
            logger.info("No ADMIN user found. Seeding default admin account.");
            
            User admin = new User();
            admin.setName("Super Admin");
            admin.setEmail("admin@smartats.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setEmailVerified(true);
            admin.setAccountStatus(UserStatus.ACTIVE);
            
            userRepository.save(admin);
            logger.info("Default admin account created successfully.");
        } else {
            logger.info("ADMIN account already exists. Skipping seeder.");
        }
    }
}
