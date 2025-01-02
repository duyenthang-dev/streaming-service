package dev.victor.streamingservice.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.StringUtils;

public class SortOrderUtil {

    public static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder camelCaseString = new StringBuilder();
        boolean nextCharUpperCase = false;

        for (char c : str.toCharArray()) {
            if (Character.isSpaceChar(c) || c == '_' || c == '-') {
                nextCharUpperCase = true;
            } else {
                if (nextCharUpperCase) {
                    camelCaseString.append(Character.toUpperCase(c));
                    nextCharUpperCase = false;
                } else {
                    camelCaseString.append(Character.toLowerCase(c));
                }
            }
        }

        return camelCaseString.toString();
    }

    public static List<Order> getSortOrder(String sortBy, boolean isNativeQuery) {
        List<Order> orders = new ArrayList<>();
        if (!StringUtils.hasText(sortBy)) {
            return orders;
        }

        Arrays.stream(sortBy.split(",")).forEach(sort -> {
            String[] pair = sort.trim().split(" ");
            if (pair.length <= 0 || pair.length > 2) {
                throw new IllegalArgumentException("Invalid sort format");
            }
            String property = pair[0];
            property = isNativeQuery ? property : toCamelCase(property);
            Direction direction = Direction.ASC;
            if (pair.length == 2 && StringUtils.hasText(pair[1]) && pair[1].equalsIgnoreCase("desc")) {
                direction = Direction.fromString(pair[1]);
            }
            orders.add(new Order(direction, property));
        });
        return orders;
    }
}
