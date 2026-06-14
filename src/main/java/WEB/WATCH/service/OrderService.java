package WEB.WATCH.service;

import WEB.WATCH.model.CartItem;
import WEB.WATCH.model.Order;
import WEB.WATCH.model.OrderDetail;
import WEB.WATCH.model.Product;
import WEB.WATCH.repository.OrderDetailRepository;
import WEB.WATCH.repository.OrderRepository;
import WEB.WATCH.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final CouponService couponService;

    public Order placeOrder(String customerName, String address, String phone, String email, String notes, String paymentMethod, String couponCode) {
        List<CartItem> cartItems = cartService.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống. Không thể đặt hàng!");
        }
        
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setShippingAddress(address);
        order.setPhoneNumber(phone);
        order.setEmail(email);
        order.setNotes(notes);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("Pending");
        order.setCouponCode(couponCode);

        double totalBeforeDiscount = 0;
        List<OrderDetail> details = new ArrayList<>();
        for (CartItem item : cartService.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));
            
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Sản phẩm " + product.getName() + " không đủ tồn kho!");
            }
            
            // Deduct stock
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            Double price = (product.getDiscountPrice() != null && product.getDiscountPrice() > 0) ? product.getDiscountPrice() : product.getPrice();
            totalBeforeDiscount += (price != null ? price : 0) * item.getQuantity();

            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(price);
            detail.setOrder(order);
            details.add(detail);
        }
        order.setOrderDetails(details);

        if (couponCode != null && !couponCode.isEmpty()) {
            final double total = totalBeforeDiscount;
            couponService.getByCode(couponCode).ifPresentOrElse(coupon -> {
                if (coupon.isValid(total)) {
                    order.setDiscountAmount(coupon.calculateDiscount(total));
                } else {
                    log.warn("Coupon {} is invalid for order total {}", couponCode, total);
                }
            }, () -> {
                log.warn("Coupon code {} not found", couponCode);
            });
        }
        
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart();
        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByEmail(email);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        String currentStatus = order.getStatus();
        
        boolean isValid = false;
        if ("Pending".equals(currentStatus) && "Confirmed".equals(status)) isValid = true;
        else if ("Confirmed".equals(currentStatus) && "Shipping".equals(status)) isValid = true;
        else if ("Shipping".equals(currentStatus) && "Completed".equals(status)) isValid = true;
        else if ("Cancelled".equals(status) && "Pending".equals(currentStatus)) isValid = true;
        
        if (!isValid && !currentStatus.equals(status)) {
            throw new IllegalArgumentException("Trạng thái chuyển đổi không hợp lệ: " + currentStatus + " -> " + status);
        }

        order.setStatus(status);
        orderRepository.save(order);
    }

    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (!"Pending".equals(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn hàng ở trạng thái Chờ xác nhận!");
        }
        
        // Restore stock
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + detail.getQuantity());
                productRepository.save(product);
            }
        }
        
        order.setStatus("Cancelled");
        orderRepository.save(order);
    }
    
    public boolean hasUserPurchasedProduct(String email, Long productId) {
        if (email == null || productId == null) return false;
        return orderRepository.hasUserPurchasedProduct(email, productId);
    }
}
