package WEB.WATCH.service;
import WEB.WATCH.Role; import
        WEB.WATCH.model.User;
import WEB.WATCH.repository.IRoleRepository;
import WEB.WATCH.repository.IUserRepository;
import jakarta.validation.constraints.NotNull; import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Lưu người dùng mới vào cơ sở dữ liệu sau khi mã hóa mật khẩu.
    public void save(@NotNull User user) {
        // Kiểm tra xem mật khẩu đã được mã hóa chưa (BCrypt bắt đầu bằng $2a$, $2b$ hoặc $2y$)
        if (user.getPassword() != null && !user.getPassword().matches("^\\$2[ayb]\\$.*")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void updateResetToken(User user, String token) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        existingUser.setResetPasswordToken(token);
        existingUser.setTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        userRepository.save(existingUser);
    }

    @Transactional
    public void updatePassword(User user, String newPassword) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Đảm bảo các trường bắt buộc không bị trống để tránh lỗi validation
        if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
            existingUser.setFullName("User_" + existingUser.getUsername());
        }
        
        existingUser.setPassword(passwordEncoder.encode(newPassword));
        existingUser.setResetPasswordToken(null);
        existingUser.setTokenExpiry(null);
        userRepository.saveAndFlush(existingUser); // Ép lưu vào DB ngay lập tức
    }
    // Gán vai trò mặc định cho người dùng dựa trên tên người dùng.
    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    user.getRoles().add(roleRepository.findRoleById(Role.USER.value));
                    userRepository.save(user);
                },
                () -> {
                    throw new UsernameNotFoundException("User not found");
                }
        );
    }

    // Tải thông tin chi tiết người dùng để xác thực.
    @Override
    public UserDetails loadUserByUsername(String username) throws
            UsernameNotFoundException {
        // Sử dụng findByUsername đã được định nghĩa bên dưới để hỗ trợ tìm theo cả username và email
        var user = findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với: " + username));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                        .collect(java.util.stream.Collectors.toList()))
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }
    // Tìm kiếm người dùng dựa trên tên đăng nhập.
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return Optional.empty();
        }
        // Try finding by username first
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            // Then try finding by email
            user = userRepository.findByEmail(username);
        }
        // Try lowercase versions just in case
        if (user.isEmpty()) {
            user = userRepository.findByUsername(username.toLowerCase());
        }
        if (user.isEmpty()) {
            user = userRepository.findByEmail(username.toLowerCase());
        }
        return user;
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Không thể xóa tài khoản Admin!");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Không thể khóa tài khoản Admin!");
        }
        boolean newStatus = !user.isEnabled();
        user.setEnabled(newStatus);
        userRepository.updateEnabledStatus(id, newStatus);
    }

    @Transactional
    public void updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        
        // Cập nhật username nếu nó là email và email thay đổi (cho đồng bộ OAuth2)
        if (existingUser.getUsername().equalsIgnoreCase(existingUser.getEmail())) {
            // existingUser.setUsername(user.getEmail()); // Tạm thời không đổi username để tránh logout
        }
        
        userRepository.save(existingUser);
    }
}
