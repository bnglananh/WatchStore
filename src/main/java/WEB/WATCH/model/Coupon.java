package WEB.WATCH.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String discountType; // PERCENTAGE, FIXED
    private Double discountValue;
    private Double minOrderAmount;
    private LocalDate expirationDate;
    private Boolean active = true;

    public boolean isValid(Double orderAmount) {
        if (!active) return false;
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) return false;
        if (minOrderAmount != null && orderAmount < minOrderAmount) return false;
        return true;
    }

    public Double calculateDiscount(Double orderAmount) {
        if (!isValid(orderAmount)) return 0.0;
        if ("PERCENTAGE".equals(discountType)) {
            return orderAmount * (discountValue / 100);
        } else {
            return Math.min(discountValue, orderAmount);
        }
    }
}
