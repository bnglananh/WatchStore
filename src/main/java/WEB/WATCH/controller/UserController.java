package WEB.WATCH.controller;
import WEB.WATCH.model.User; import
        WEB.WATCH.service.UserService; import
        jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull; import
        lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import WEB.WATCH.dto.PasswordChangeRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller; import
        org.springframework.ui.Model;
import org.springframework.validation.BindingResult; import
        org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller // Đánh dấu lớp này là một Controller trong Spring MVC.
@RequestMapping("/")
@RequiredArgsConstructor

public class UserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "users/login";
    }
    @GetMapping("/register")
    public String register(@NotNull Model model) {
        model.addAttribute("user", new User()); // Thêm một đối tượng User mới vào model
        return "users/register";
    }
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user, // Validate đối tượng User
@NotNull BindingResult bindingResult, // Kết quả của quá trình validate
Model model) {
        if (bindingResult.hasErrors()) { // Kiểm tra nếu có lỗi validate
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            return "users/register"; // Trả về lại view "register" nếu có lỗi
        }
        userService.save(user); // Lưu người dùng vào cơ sở dữ liệu
        userService.setDefaultRole(user.getUsername()); // Gán vai trò mặc định cho người dùng
        return "redirect:/login"; // Chuyển hướng người dùng tới trang "login"
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = null;
        String principalName = authentication.getName();

        // 1. Try to find by principal name (usually email for this app)
        user = userService.findByUsername(principalName).orElse(null);

        // 2. Fallback for OAuth2: check principal attributes
        if (user == null && authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauth2User = oauthToken.getPrincipal();
            String email = oauth2User.getAttribute("email");
            if (email != null) {
                user = userService.findByEmail(email.toLowerCase()).orElse(null);
            }
            
            // 3. Last resort: check other attributes if email didn't work
            if (user == null) {
                String name = oauth2User.getAttribute("name");
                if (name != null) {
                    user = userService.findByUsername(name).orElse(null);
                }
            }
        }

        if (user == null) {
            // Log for debugging
            System.out.println("DEBUG: Profile lookup failed for principal: " + principalName);
            return "redirect:/login?error=user_not_found";
        }

        model.addAttribute("user", user);
        if (!model.containsAttribute("passwordChangeRequest")) {
            model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
        }
        return "users/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("user") User user,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
            return "users/profile";
        }
        try {
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            model.addAttribute("errors", new String[]{e.getMessage()});
            model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
            return "users/profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeRequest") PasswordChangeRequest request,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            // Need to reload user info for the view
            User user = userService.findByUsername(authentication.getName()).orElse(null);
            model.addAttribute("user", user);
            return "users/profile";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp");
            User user = userService.findByUsername(authentication.getName()).orElse(null);
            model.addAttribute("user", user);
            return "users/profile";
        }

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                bindingResult.rejectValue("currentPassword", "error.currentPassword", "Mật khẩu hiện tại không chính xác");
                model.addAttribute("user", user);
                return "users/profile";
            }

            userService.updatePassword(user, request.getNewPassword());
            redirectAttributes.addFlashAttribute("message", "Đổi mật khẩu thành công!");
        }

        return "redirect:/profile";
    }
}
