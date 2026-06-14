package WEB.WATCH.repository;

import WEB.WATCH.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndApprovedTrue(Long productId);
    List<Review> findByApprovedFalse();

    @Query("SELECT r.product.id, AVG(r.rating) FROM Review r WHERE r.approved = true GROUP BY r.product.id")
    List<Object[]> getAllAverageRatings();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double getAverageRating(@Param("productId") Long productId);
}
