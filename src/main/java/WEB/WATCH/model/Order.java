package WEB.WATCH.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;
    private String shippingAddress;
    private String phoneNumber;
    private String email;
    private String notes;
    private String paymentMethod;
    private String status; // Pending, Confirmed, Shipping, Completed
    private String couponCode;
    private Double discountAmount = 0.0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    public double getTotalBeforeDiscount() {
        if (orderDetails == null) return 0;
        return orderDetails.stream()
                .filter(d -> d.getProduct() != null)
                .mapToDouble(d -> {
                    if (d.getPrice() != null) return d.getPrice() * d.getQuantity();
                    Double price = (d.getProduct().getDiscountPrice() != null && d.getProduct().getDiscountPrice() > 0) 
                                   ? d.getProduct().getDiscountPrice() 
                                   : d.getProduct().getPrice();
                    return (price != null ? price : 0) * d.getQuantity();
                })
                .sum();
    }

    public double getTotalPrice() {
        double total = getTotalBeforeDiscount();
        return Math.max(0, total - (discountAmount != null ? discountAmount : 0));
    }
}
