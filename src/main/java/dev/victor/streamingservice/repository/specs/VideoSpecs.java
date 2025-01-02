package dev.victor.streamingservice.repository.specs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import dev.victor.streamingservice.model.entity.Video;
import jakarta.persistence.criteria.Predicate;

public class VideoSpecs {
    public static Specification<Video> filterBy(String search, String conversionStatus) {
        return (root, query, criteriaBuilder) -> {
            if (search == null && conversionStatus == null) {
                return null;
            }

            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(conversionStatus)) {
                predicates.add(criteriaBuilder.equal(root.get("conversionStatus"), conversionStatus));
            }

            if (StringUtils.hasText(search)) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
                                "%" + search.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),
                                "%" + search.toLowerCase() + "%")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
