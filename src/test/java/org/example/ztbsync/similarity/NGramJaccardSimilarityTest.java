package org.example.ztbsync.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class NGramJaccardSimilarityTest {

    private final NGramJaccardSimilarity similarity = new NGramJaccardSimilarity();

    @Test
    void calculatesSetJaccardSimilarity() {
        double score = similarity.similarity(Set.of("abc", "bcd", "cde"), Set.of("abc", "bcd", "def"));

        assertThat(score).isEqualTo(0.5);
    }
}
