package WEB.WATCH.controller;

import WEB.WATCH.service.ProductService;
import WEB.WATCH.service.CategoryService;
import WEB.WATCH.model.Product;
import WEB.WATCH.model.Coupon;
import WEB.WATCH.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final WEB.WATCH.service.ReviewService reviewService;

    @GetMapping("/")
    public String index(Model model) {
        List<Product> allProducts = productService.getAllProducts();

        // 1. Featured Products
        List<Product> featuredProducts = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getFeatured()))
                .limit(4)
                .collect(Collectors.toList());

        // 2. New Arrivals (Latest 8)
        List<Product> newArrivals = allProducts.stream()
                .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
                .limit(8)
                .collect(Collectors.toList());

        // 3. Special Offers (Discounted)
        List<Product> specialOffers = allProducts.stream()
                .filter(p -> p.getDiscountPrice() != null && p.getDiscountPrice() < p.getPrice())
                .limit(4)
                .collect(Collectors.toList());

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("newArrivals", newArrivals);
        model.addAttribute("specialOffers", specialOffers);
        model.addAttribute("categories", categoryService.getAllCategories());

        // 4. Promo Code for Header
        String promoCode = couponService.getAllCoupons().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .map(Coupon::getCode)
                .findFirst()
                .orElse("WATCH10"); // Fallback
        model.addAttribute("promoCode", promoCode);

        // 5. Ratings
        Map<Long, Double> ratings = reviewService.getAllAverageRatings();
        model.addAttribute("ratings", ratings);

        return "user/index";
    }
}
