package org.example.ztbsync.rag;

import java.util.List;

public record RagChunk(
        int chunkIndex,
        String chunkText,
        String sectionPath,
        List<String> blockTypes,
        boolean boilerplate,
        int charStart,
        int charEnd) {
}
