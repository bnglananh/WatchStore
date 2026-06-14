package WEB.WATCH.controller;

import WEB.WATCH.model.Order;
import WEB.WATCH.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(@RequestParam(required = false) String email, Model model) {
        if (email != null && !email.isEmpty()) {
            model.addAttribute("orders", orderService.getOrdersByEmail(email));
            model.addAttribute("searchEmail", email);
        } else {
            model.addAttribute("orders", orderService.getAllOrders());
        }
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        if (order == null) return "redirect:/admin/orders";
        model.addAttribute("order", order);
        return "admin/orders/detail";
    }

    @PostMapping("/update-status")
    public String updateStatus(@RequestParam Long orderId, @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
        return "redirect:/admin/orders/" + orderId;
    }

    @GetMapping("/invoice/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        if (order == null) return "redirect:/admin/orders";
        model.addAttribute("order", order);
        return "admin/orders/invoice";
    }
}
