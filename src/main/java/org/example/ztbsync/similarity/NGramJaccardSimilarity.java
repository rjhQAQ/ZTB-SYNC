package org.example.ztbsync.similarity;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class NGramJaccardSimilarity {

    public double similarity(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        if (union.isEmpty()) {
            return 0;
        }
        return (double) intersection.size() / union.size();
    }
}
