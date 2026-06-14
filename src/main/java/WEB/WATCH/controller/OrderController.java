package WEB.WATCH.controller;

import WEB.WATCH.model.Order;
import WEB.WATCH.service.CartService;
import WEB.WATCH.service.OrderService;
import WEB.WATCH.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping("/checkout")
    public String checkout(Model model, java.security.Principal principal) {
        if (cartService.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        
        if (principal != null) {
            String username = principal.getName();
            userService.findByUsername(username).ifPresent(user -> {
                model.addAttribute("customerName", user.getUsername());
                model.addAttribute("email", user.getEmail());
                model.addAttribute("phoneNumber", user.getPhone());
            });
        }
        
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("discountAmount", cartService.getDiscountAmount());
        model.addAttribute("grandTotal", cartService.getGrandTotal());
        model.addAttribute("couponCode", cartService.getAppliedCoupon() != null ? cartService.getAppliedCoupon().getCode() : "");
        
        return "user/cart/checkout";
    }

    @PostMapping("/place")
    public String placeOrder(@RequestParam String customerName,
                             @RequestParam String shippingAddress,
                             @RequestParam String phoneNumber,
                             @RequestParam String email,
                             @RequestParam(required = false) String notes,
                             @RequestParam String paymentMethod,
                             @RequestParam(required = false) String couponCode,
                             Model model) {
        try {
            Order order = orderService.placeOrder(customerName, shippingAddress, phoneNumber, email, notes, paymentMethod, couponCode);
            model.addAttribute("order", order);
            return "user/cart/order-confirmation";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("totalPrice", cartService.getTotalPrice());
            model.addAttribute("discountAmount", cartService.getDiscountAmount());
            model.addAttribute("grandTotal", cartService.getGrandTotal());
            model.addAttribute("customerName", customerName);
            model.addAttribute("email", email);
            model.addAttribute("phoneNumber", phoneNumber);
            model.addAttribute("shippingAddress", shippingAddress);
            model.addAttribute("couponCode", couponCode);
            return "user/cart/checkout";
        }
    }

    @PostMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return "redirect:/order/history";
    }

    @GetMapping("/history")
    public String orderHistory(@RequestParam(required = false) String email, 
                               Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()));
        
        if (isAuthenticated) {
            boolean isAdmin = auth.getAuthorities().stream()
                                  .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            String userEmail = null;
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                userEmail = userService.findByUsername(((UserDetails) principal).getUsername())
                                       .map(WEB.WATCH.model.User::getEmail).orElse(null);
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                userEmail = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            }

            model.addAttribute("isUserLoggedIn", true);
            model.addAttribute("isAdmin", isAdmin);

            if (isAdmin && email != null && !email.isEmpty()) {
                // Admin can search any email
                model.addAttribute("orders", orderService.getOrdersByEmail(email));
                model.addAttribute("searchEmail", email);
            } else if (userEmail != null) {
                // Regular user (or admin without search) sees their own orders
                model.addAttribute("orders", orderService.getOrdersByEmail(userEmail));
                model.addAttribute("searchEmail", userEmail);
            } else {
                model.addAttribute("orders", new java.util.ArrayList<>());
            }
        } else {
            // Guest tracking
            model.addAttribute("isUserLoggedIn", false);
            if (email != null && !email.isEmpty()) {
                model.addAttribute("orders", orderService.getOrdersByEmail(email));
                model.addAttribute("searchEmail", email);
            } else {
                model.addAttribute("orders", new java.util.ArrayList<>());
            }
        }
        
        return "user/order/history";
    }
}
