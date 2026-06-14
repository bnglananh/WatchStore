package WEB.WATCH.controller;

import WEB.WATCH.model.Product;
import WEB.WATCH.model.Coupon;
import WEB.WATCH.service.CartService;
import WEB.WATCH.service.ProductService;
import WEB.WATCH.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final ProductService productService;
    private final CouponService couponService;

    @GetMapping("/count")
    @ResponseBody
    public Map<String, Integer> getCartCount() {
        return Map.of("count", cartService.getItems().size());
    }

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("discountAmount", cartService.getDiscountAmount());
        model.addAttribute("grandTotal", cartService.getGrandTotal());
        model.addAttribute("appliedCoupon", cartService.getAppliedCoupon());
        model.addAttribute("coupons", couponService.getAllCoupons().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .toList());
        return "user/cart/index";
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<?> addToCart(@PathVariable Long id, @RequestParam(defaultValue = "1") int quantity) {
        try {
            Product product = productService.getProductById(id);
            if (product != null) {
                cartService.addToCart(product, quantity);
                return ResponseEntity.ok()
                        .body(Map.of("message", "Đã thêm vào giỏ hàng", "cartCount", cartService.getItems().size()));
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateQuantity(@PathVariable Long id, @RequestParam int quantity) {
        try {
            cartService.updateQuantity(id, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("totalPrice", cartService.getTotalPrice());
            response.put("discountAmount", cartService.getDiscountAmount());
            response.put("grandTotal", cartService.getGrandTotal());
            response.put("cartCount", cartService.getItems().size());
            response.put("couponValid", cartService.getAppliedCoupon() != null);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id) {
        cartService.removeFromCart(id);
        return "redirect:/cart";
    }

    @GetMapping("/clear")
    public String clearCart() {
        cartService.clearCart();
        return "redirect:/cart";
    }

    @PostMapping("/apply-coupon")
    @ResponseBody
    public ResponseEntity<?> applyCoupon(@RequestParam String code) {
        return couponService.getByCode(code)
                .map(coupon -> {
                    if (coupon.isValid(cartService.getTotalPrice())) {
                        cartService.applyCoupon(coupon);
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("discountAmount", cartService.getDiscountAmount());
                        response.put("grandTotal", cartService.getGrandTotal());
                        response.put("code", coupon.getCode());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body("Mã giảm giá không hợp lệ hoặc không đủ điều kiện (Tối thiểu " + coupon.getMinOrderAmount() + " VNĐ)");
                    }
                })
                .orElse(ResponseEntity.badRequest().body("Mã giảm giá không tồn tại!"));
    }

    @PostMapping("/remove-coupon")
    @ResponseBody
    public ResponseEntity<?> removeCoupon() {
        cartService.removeCoupon();
        Map<String, Object> response = new HashMap<>();
        response.put("totalPrice", cartService.getTotalPrice());
        response.put("grandTotal", cartService.getGrandTotal());
        return ResponseEntity.ok(response);
    }
}
