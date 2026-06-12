package org.example.ztbsync.llm;

import org.example.ztbsync.extraction.BidExtraction;
import org.example.ztbsync.extraction.TenderExtraction;

public record LlmExtractionResult(
        TenderExtraction tenderExtraction,
        BidExtraction bidExtraction,
        String rawJson) {

    public static LlmExtractionResult empty() {
        return new LlmExtractionResult(new TenderExtraction(), new BidExtraction(), null);
    }
}
