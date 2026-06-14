package WEB.WATCH.repository;
import WEB.WATCH.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByEmail(String email);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderDetails od WHERE o.email = :email AND o.status = 'Completed' AND od.product.id = :productId")
    boolean hasUserPurchasedProduct(@org.springframework.data.repository.query.Param("email") String email, @org.springframework.data.repository.query.Param("productId") Long productId);
}
