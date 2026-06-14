package WEB.WATCH.controller;

import WEB.WATCH.service.ProductService;
import WEB.WATCH.service.CategoryService;
import WEB.WATCH.model.Product;
import WEB.WATCH.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UserProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;

    @GetMapping({"/products", "/user/products"})
    public String listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            Model model) {
        
        List<Product> products = productService.getAllProducts();

        // 1. Filtering logic
        if (search != null && !search.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }
        if (brand != null && !brand.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getBrand() != null && p.getBrand().equalsIgnoreCase(brand))
                    .collect(Collectors.toList());
        }
        if (minPrice != null) {
            products = products.stream().filter(p -> p.getPrice() >= minPrice).collect(Collectors.toList());
        }
        if (maxPrice != null) {
            products = products.stream().filter(p -> p.getPrice() <= maxPrice).collect(Collectors.toList());
        }

        // 2. Sorting logic
        // ... (sorting switch remains the same)
        switch (sortBy) {
            case "priceAsc":
                products.sort((p1, p2) -> Double.compare(
                    p1.getDiscountPrice() != null && p1.getDiscountPrice() > 0 ? p1.getDiscountPrice() : p1.getPrice(),
                    p2.getDiscountPrice() != null && p2.getDiscountPrice() > 0 ? p2.getDiscountPrice() : p2.getPrice()
                ));
                break;
            case "priceDesc":
                products.sort((p1, p2) -> Double.compare(
                    p2.getDiscountPrice() != null && p2.getDiscountPrice() > 0 ? p2.getDiscountPrice() : p2.getPrice(),
                    p1.getDiscountPrice() != null && p1.getDiscountPrice() > 0 ? p1.getDiscountPrice() : p1.getPrice()
                ));
                break;
            case "popularity":
                products.sort((p1, p2) -> Boolean.compare(p2.getFeatured() != null && p2.getFeatured(), p1.getFeatured() != null && p1.getFeatured()));
                break;
            case "newest":
            default:
                products.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));
                break;
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        List<String> defaultBrands = List.of("Seiko", "Casio", "Citizen", "Orient");
        List<String> existingBrands = productService.getAllProducts().stream()
                .map(Product::getBrand)
                .filter(b -> b != null && !b.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        java.util.Set<String> allBrands = new java.util.LinkedHashSet<>(defaultBrands);
        allBrands.addAll(existingBrands);
        model.addAttribute("brands", new java.util.ArrayList<>(allBrands));
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("brand", brand);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);

        // 3. Ratings
        Map<Long, Double> ratings = reviewService.getAllAverageRatings();
        model.addAttribute("ratings", ratings);
        
        return "user/products/list";
    }

    @GetMapping("/products/detail/test")
    @org.springframework.web.bind.annotation.ResponseBody
    public String productDetailTest() {
        return "Test OK";
    }

    @GetMapping("/products/detail/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        if (product == null) return "redirect:/user/products";
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviewService.getApprovedReviewsByProduct(id));
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id, 4));
        return "user/products/detail";
    }
}
