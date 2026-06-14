package WEB.WATCH.controller;

import WEB.WATCH.model.Product;
import WEB.WATCH.model.Review;
import WEB.WATCH.model.User;
import WEB.WATCH.service.ProductService;
import WEB.WATCH.service.ReviewService;
import WEB.WATCH.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import WEB.WATCH.service.OrderService;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ProductService productService;
    private final UserService userService;
    private final OrderService orderService;

    @PostMapping("/add")
    public String addReview(@RequestParam Long productId,
                            @RequestParam int rating,
                            @RequestParam String comment,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đánh giá.");
            return "redirect:/login";
        }

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        Product product = productService.getProductById(productId);

        if (user != null && product != null) {
            boolean hasPurchased = orderService.hasUserPurchasedProduct(user.getEmail(), productId);
            if (!hasPurchased) {
                redirectAttributes.addFlashAttribute("error", "Bạn chỉ có thể đánh giá sản phẩm sau khi mua và nhận hàng thành công.");
                return "redirect:/order/history";
            }

            Review review = new Review();
            review.setUser(user);
            review.setProduct(product);
            review.setRating(rating);
            review.setComment(comment);
            review.setApproved(true);
            
            reviewService.saveReview(review);
            redirectAttributes.addFlashAttribute("message", "Cảm ơn bạn! Đánh giá của bạn đã được đăng thành công.");
        }

        return "redirect:/order/history";
    }
}
