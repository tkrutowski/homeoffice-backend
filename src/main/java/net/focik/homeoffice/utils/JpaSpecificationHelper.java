package net.focik.homeoffice.utils;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class JpaSpecificationHelper {

    public static <T> Specification<T> byDate(LocalDate date, String dateComparisonType, String fieldName) {
        return (root, query, cb) -> switch (dateComparisonType) {
            case "AFTER" -> cb.greaterThan(root.get(fieldName), date);
            case "BEFORE" -> cb.lessThan(root.get(fieldName), date);
            default -> cb.equal(root.get(fieldName), date);
        };
    }

    public static <T> Specification<T> byAmount(BigDecimal amount, String amountComparisonType, String fieldName) {
        return (root, query, cb) -> switch (amountComparisonType) {
            case "GREATER" -> cb.greaterThan(root.get(fieldName), amount);
            case "LESS" -> cb.lessThan(root.get(fieldName), amount);
            default -> cb.equal(root.get(fieldName), amount);
        };
    }
}
