package WEB.WATCH.controller;

import WEB.WATCH.model.User;
import WEB.WATCH.service.EmailService;
import WEB.WATCH.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "users/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String identifier, Model model) {
        User user = null;
        boolean isEmail = identifier.contains("@");

        if (isEmail) {
            user = userService.findByEmail(identifier).orElse(null);
        } else {
            user = userService.findByUsername(identifier).orElse(null);
        }

        if (user != null) {
            if (isEmail) {
                // Email method: Generate code and send email
                String code = String.valueOf((int) (Math.random() * 900000) + 100000);
                userService.updateResetToken(user, code);
                try {
                    emailService.sendResetPasswordCode(user.getEmail(), code);
                    return "redirect:/verify-code?email=" + user.getEmail();
                } catch (Exception e) {
                    model.addAttribute("error", "Lỗi khi gửi email: " + e.getMessage());
                }
            } else {
                // Username method: Bypass email sending and redirect directly to reset password form
                return "redirect:/reset-password?email=" + user.getEmail();
            }
        } else {
            model.addAttribute("error", "Không tìm thấy người dùng với thông tin này.");
        }
        return "users/forgot-password";
    }

    @GetMapping("/verify-code")
    public String showVerifyCodeForm(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "users/verify-code";
    }

    @PostMapping("/verify-code")
    public String processVerifyCode(@RequestParam String email, @RequestParam String code, Model model) {
        User user = userService.findByEmail(email).orElse(null);
        if (user != null && code.equals(user.getResetPasswordToken())) {
            if (user.getTokenExpiry().isAfter(LocalDateTime.now())) {
                return "redirect:/reset-password?token=" + code;
            } else {
                model.addAttribute("error", "Mã xác nhận đã hết hạn.");
            }
        } else {
            model.addAttribute("error", "Mã xác nhận không chính xác.");
        }
        model.addAttribute("email", email);
        return "users/verify-code";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String token,
                                       @RequestParam(required = false) String email,
                                       Model model) {
        User user = null;
        if (token != null && !token.isEmpty()) {
            user = userService.getAllUsers().stream()
                    .filter(u -> token.equals(u.getResetPasswordToken()))
                    .findFirst()
                    .orElse(null);
            
            if (user == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
                model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn.");
                return "users/forgot-password";
            }
            model.addAttribute("token", token);
        } else if (email != null && !email.isEmpty()) {
            user = userService.findByEmail(email).orElse(null);
            if (user == null) {
                model.addAttribute("error", "Email không hợp lệ.");
                return "users/forgot-password";
            }
            model.addAttribute("email", email);
        } else {
            return "redirect:/forgot-password";
        }

        return "users/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam(required = false) String token,
                                       @RequestParam(required = false) String email,
                                       @RequestParam String password,
                                       Model model) {
        User user = null;
        if (token != null && !token.isEmpty()) {
            user = userService.getAllUsers().stream()
                    .filter(u -> token.equals(u.getResetPasswordToken()))
                    .findFirst()
                    .orElse(null);
        } else if (email != null && !email.isEmpty()) {
            user = userService.findByEmail(email).orElse(null);
        }

        if (user != null) {
            userService.updatePassword(user, password);
            return "redirect:/login?resetSuccess";
        }
        
        return "redirect:/forgot-password?error";
    }
}
