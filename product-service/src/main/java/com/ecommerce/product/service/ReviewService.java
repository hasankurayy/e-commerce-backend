package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.product.client.OrderFeignClient;
import com.ecommerce.product.dto.ReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.dto.ReviewSummaryResponse;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderFeignClient orderFeignClient;

    @Transactional
    public ReviewResponse addReview(Long productId, Long userId, String userEmail, ReviewRequest request) {
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BusinessException("Bu ürünü zaten değerlendirdiniz");
        }

        var hasOrdered = orderFeignClient.hasOrdered(userId, productId);
        if (!Boolean.TRUE.equals(hasOrdered.data())) {
            throw new BusinessException("Yalnızca satın aldığınız ürünleri değerlendirebilirsiniz");
        }

        String userName = userEmail.contains("@")
                ? userEmail.substring(0, userEmail.indexOf('@'))
                : userEmail;

        Review review = Review.builder()
                .productId(productId)
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return toResponse(reviewRepository.save(review));
    }

    public ReviewSummaryResponse getSummary(Long productId, Long userId) {
        int total = reviewRepository.countByProductId(productId);
        double avg = total == 0 ? 0.0
                : (double) reviewRepository.sumRatingByProductId(productId) / total;

        List<ReviewResponse> last5 = reviewRepository
                .findTop5ByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toResponse).toList();

        ReviewResponse myReview = userId == null ? null
                : reviewRepository.findByProductIdAndUserId(productId, userId)
                        .map(this::toResponse).orElse(null);

        return new ReviewSummaryResponse(
                Math.round(avg * 10.0) / 10.0,
                total,
                last5,
                myReview
        );
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getUserId(), r.getUserEmail(),
                r.getUserName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
