package WEB.WATCH;

import WEB.WATCH.model.Role;
import WEB.WATCH.model.User;
import WEB.WATCH.repository.IRoleRepository;
import WEB.WATCH.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final WEB.WATCH.repository.CouponRepository couponRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize Coupons
        if (couponRepository.count() == 0) {
            WEB.WATCH.model.Coupon c1 = new WEB.WATCH.model.Coupon();
            c1.setCode("WELCOME10");
            c1.setDiscountType("PERCENTAGE");
            c1.setDiscountValue(10.0);
            c1.setMinOrderAmount(0.0);
            c1.setActive(true);
            couponRepository.save(c1);

            WEB.WATCH.model.Coupon c2 = new WEB.WATCH.model.Coupon();
            c2.setCode("SAVE50");
            c2.setDiscountType("FIXED");
            c2.setDiscountValue(50000.0);
            c2.setMinOrderAmount(500000.0);
            c2.setActive(true);
            couponRepository.save(c2);
        }

        // Initialize Roles
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().id(1L).name("ROLE_ADMIN").description("Quản trị viên").build());
            roleRepository.save(Role.builder().id(2L).name("ROLE_USER").description("Người dùng").build());
        }

        // Initialize Admin User
        userRepository.findByUsername("admin").ifPresentOrElse(
                admin -> {
                    boolean updated = false;
                    Role adminRole = roleRepository.findRoleById(1L);
                    if (!admin.getRoles().contains(adminRole)) {
                        admin.getRoles().add(adminRole);
                        updated = true;
                    }
                    if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                        admin.setPassword(passwordEncoder.encode("admin123"));
                        updated = true;
                    }
                    if (updated) {
                        userRepository.save(admin);
                    }
                },
                () -> {
                    Role adminRole = roleRepository.findRoleById(1L);
                    User admin = User.builder()
                            .username("admin")
                            .fullName("Administrator")
                            .password(passwordEncoder.encode("admin123"))
                            .email("admin@watchstore.com")
                            .phone("0123456789")
                            .roles(new java.util.HashSet<>(java.util.Set.of(adminRole)))
                            .enabled(true)
                            .build();
                    userRepository.save(admin);
                });
    }
}
