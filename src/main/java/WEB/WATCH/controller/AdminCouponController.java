package WEB.WATCH.controller;

import WEB.WATCH.model.Coupon;
import WEB.WATCH.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;

    @GetMapping
    public String listCoupons(Model model) {
        model.addAttribute("coupons", couponRepository.findAll());
        return "admin/coupons/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("coupon", new Coupon());
        return "admin/coupons/add";
    }

    @PostMapping("/save")
    public String saveCoupon(@ModelAttribute Coupon coupon) {
        couponRepository.save(coupon);
        return "redirect:/admin/coupons";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon Id:" + id));
        model.addAttribute("coupon", coupon);
        return "admin/coupons/edit";
    }

    @PostMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id) {
        couponRepository.deleteById(id);
        return "redirect:/admin/coupons";
    }
}
