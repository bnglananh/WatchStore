package WEB.WATCH.repository;
import WEB.WATCH.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndIdNot(Long categoryId, Long productId);
    List<Product> findByBrandAndIdNot(String brand, Long productId);
}
