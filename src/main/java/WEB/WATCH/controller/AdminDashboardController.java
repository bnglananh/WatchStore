package WEB.WATCH.controller;

import WEB.WATCH.service.OrderService;
import WEB.WATCH.service.ProductService;
import WEB.WATCH.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var allOrders = orderService.getAllOrders();

        // Tổng doanh thu từ đơn hàng hoàn thành
        double totalRevenue = allOrders.stream()
                .filter(o -> "Completed".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalPrice())
                .sum();

        // Đơn hàng mới (Pending)
        long newOrdersCount = allOrders.stream()
                .filter(o -> "Pending".equals(o.getStatus()))
                .count();

        // Tổng đơn hoàn thành
        long completedOrdersCount = allOrders.stream()
                .filter(o -> "Completed".equals(o.getStatus()))
                .count();

        var allProducts = productService.getAllProducts();
        var lowStockProducts = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() < 5)
                .toList();

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", completedOrdersCount);
        model.addAttribute("newOrders", newOrdersCount);
        model.addAttribute("totalProducts", allProducts.size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("allProducts", allProducts); // Thêm tất cả sản phẩm để xem tồn kho

        // 6. Dữ liệu tồn kho an toàn cho Chart.js và Tìm kiếm/Bộ lọc động
        List<Map<String, Object>> inventoryData = allProducts.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("brand", p.getBrand() != null ? p.getBrand() : "Khác");
                    map.put("category", p.getCategory() != null ? p.getCategory().getName() : "Chưa phân loại");
                    map.put("stock", p.getStock() != null ? p.getStock() : 0);
                    map.put("price", p.getPrice() != null ? p.getPrice() : 0.0);
                    map.put("image", p.getImage());
                    return map;
                })
                .toList();
        model.addAttribute("inventoryData", inventoryData);

        // ===== CHART DATA =====

        // 1. Doanh thu theo tháng (phân bổ đơn hàng theo khoảng ID, vì Order không có createdAt)
        LocalDate today = LocalDate.now();
        List<String> monthLabels = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today.minusMonths(i));
            monthLabels.add("T" + ym.getMonthValue() + "/" + String.valueOf(ym.getYear()).substring(2));
        }

        long maxId = allOrders.isEmpty() ? 1 : allOrders.stream().mapToLong(o -> o.getId()).max().orElse(1);
        long minId = allOrders.isEmpty() ? 0 : allOrders.stream().mapToLong(o -> o.getId()).min().orElse(0);
        long idRange = Math.max(1, maxId - minId);

        List<Double> monthlyRevenue = new ArrayList<>();
        for (int bucket = 0; bucket < 6; bucket++) {
            final int b = bucket;
            double rev = allOrders.stream()
                    .filter(o -> "Completed".equals(o.getStatus()))
                    .filter(o -> {
                        long norm = (long)(((o.getId() - minId) * 6.0) / idRange);
                        return norm == b || (b == 5 && norm >= 5);
                    })
                    .mapToDouble(o -> o.getTotalPrice())
                    .sum();
            monthlyRevenue.add(rev);
        }
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthlyRevenue", monthlyRevenue);

        // 2. Phân bổ đơn hàng theo trạng thái
        List<String> statusLabels = List.of("Chờ xác nhận", "Đã xác nhận", "Đang giao", "Hoàn thành", "Đã hủy");
        List<String> statusKeys   = List.of("Pending", "Confirmed", "Shipping", "Completed", "Cancelled");
        List<Long> statusCounts = new ArrayList<>();
        for (String key : statusKeys) {
            statusCounts.add(allOrders.stream().filter(o -> key.equals(o.getStatus())).count());
        }
        model.addAttribute("statusLabels", statusLabels);
        model.addAttribute("statusCounts", statusCounts);

        // 3. Top 5 sản phẩm bán chạy
        Map<String, Long> salesMap = new LinkedHashMap<>();
        allOrders.stream()
                .filter(o -> o.getOrderDetails() != null)
                .flatMap(o -> o.getOrderDetails().stream())
                .filter(d -> d.getProduct() != null)
                .forEach(d -> salesMap.merge(d.getProduct().getName(), (long) d.getQuantity(), Long::sum));

        List<Map.Entry<String, Long>> top5 = salesMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("topProductNames", top5.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        model.addAttribute("topProductSales", top5.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

        // 4. Sản phẩm theo danh mục
        Map<String, Long> catMap = allProducts.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getName(), Collectors.counting()));
        model.addAttribute("categoryNames", new ArrayList<>(catMap.keySet()));
        model.addAttribute("categoryCounts", new ArrayList<>(catMap.values()));
        
        // 5. Phân bổ người dùng theo nhà cung cấp (Provider)
        Map<String, Long> userProviderMap = userService.getAllUsers().stream()
                .collect(Collectors.groupingBy(user -> user.getProvider() != null ? user.getProvider() : "LOCAL", Collectors.counting()));
        model.addAttribute("userProviderLabels", new ArrayList<>(userProviderMap.keySet()));
        model.addAttribute("userProviderCounts", new ArrayList<>(userProviderMap.values()));

        return "admin/dashboard";
    }
}
