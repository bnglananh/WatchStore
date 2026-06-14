package WEB.WATCH.service;

import WEB.WATCH.model.Review;
import WEB.WATCH.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public List<Review> getApprovedReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdAndApprovedTrue(productId);
    }

    public List<Review> getPendingReviews() {
        return reviewRepository.findByApprovedFalse();
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }

    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review != null) {
            review.setApproved(true);
            reviewRepository.save(review);
        }
    }

    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    public double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRating(productId);
        return avg != null ? avg : 0.0;
    }

    public Map<Long, Double> getAllAverageRatings() {
        return reviewRepository.getAllAverageRatings().stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Double) obj[1],
                        (a, b) -> a
                ));
    }
}
