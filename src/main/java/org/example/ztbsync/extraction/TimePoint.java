package org.example.ztbsync.extraction;

public record TimePoint(
        String label,
        String originalText,
        String normalizedTime,
        String source) {
}
