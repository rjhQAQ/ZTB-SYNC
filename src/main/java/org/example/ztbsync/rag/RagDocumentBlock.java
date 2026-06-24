package org.example.ztbsync.rag;

public record RagDocumentBlock(
        int index,
        RagBlockType type,
        String text,
        int headingLevel,
        int charStart,
        int charEnd) {

    public boolean heading() {
        return type == RagBlockType.HEADING;
    }
}
