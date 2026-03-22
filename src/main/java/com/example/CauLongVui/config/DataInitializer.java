package com.example.CauLongVui.config;

import com.example.CauLongVui.entity.Court;
import com.example.CauLongVui.entity.Court.CourtStatus;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.entity.User.Role;
import com.example.CauLongVui.repository.CourtRepository;
import com.example.CauLongVui.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // 1. Tao tai khoan Admin mac dinh neu chua co
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .fullName("Quan Tri Vien")
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("admin@123"))
                    .phone("0999888777")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin account created: admin@gmail.com");
        } else {
            log.info("Admin account already exists.");
        }

        // 2. Tao du lieu san mau neu chua co
        if (courtRepository.count() == 0) {
            log.info("No courts found. Seeding sample courts...");

            List<Court> courts = List.of(
                    Court.builder()
                            .name("San A1")
                            .description("San tieu chuan tang 1 - Anh sang tot, thong thoang")
                            .pricePerHour(90000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San A2")
                            .description("San tieu chuan tang 1 - Gan cua ra vao")
                            .pricePerHour(90000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San A3")
                            .description("San tieu chuan tang 1 - San goc yen tinh")
                            .pricePerHour(90000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San B1")
                            .description("San VIP tang 2 - Dieu hoa, ghe ngoi thoai mai")
                            .pricePerHour(150000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build(),
                    Court.builder()
                            .name("San B2")
                            .description("San VIP tang 2 - View dep nhin ra san ngoai")
                            .pricePerHour(150000.0)
                            .status(CourtStatus.AVAILABLE)
                            .build());

            courtRepository.saveAll(courts);
            log.info("Seeded {} courts successfully.", courts.size());
        } else {
            log.info("Courts already exist ({}). Skipping seed.", courtRepository.count());
        }
    }
}
