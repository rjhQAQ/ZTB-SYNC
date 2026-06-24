package org.example.ztbsync.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.example.ztbsync.config.ZtbProperties;
import org.springframework.stereotype.Component;

@Component
public class RagChunker {

    private static final Pattern PAGE_NOISE = Pattern.compile("^第?\\d+页(?:共\\d+页)?$");
    private static final List<String> BOILERPLATE_KEYWORDS = List.of(
            "授权委托书", "法定代表人", "投标函", "承诺函", "声明", "签字", "盖章", "正本", "副本");

    private final ZtbProperties properties;

    public RagChunker(ZtbProperties properties) {
        this.properties = properties;
    }

    public List<RagChunk> chunk(List<RagDocumentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        ChunkState state = new ChunkState();
        List<RagChunk> chunks = new ArrayList<>();
        List<String> sectionStack = new ArrayList<>();
        int[] chunkIndex = new int[] {0};

        for (RagDocumentBlock block : blocks) {
            if (block.text() == null || block.text().isBlank() || PAGE_NOISE.matcher(block.text()).matches()) {
                continue;
            }
            if (block.heading()) {
                flush(chunks, state, sectionPath(sectionStack), chunkIndex, false);
                updateSection(sectionStack, block.headingLevel(), block.text());
                continue;
            }
            boolean boilerplate = isBoilerplate(block.text());
            if (properties.getEmbedding().isFilterBoilerplate()
                    && boilerplate
                    && !properties.getEmbedding().isKeepBoilerplate()) {
                continue;
            }
            if (block.text().length() > properties.getEmbedding().getMaxChunkChars()) {
                flush(chunks, state, sectionPath(sectionStack), chunkIndex, true);
                splitLongBlock(chunks, block, sectionPath(sectionStack), boilerplate, chunkIndex);
                continue;
            }
            if (state.length() > 0
                    && state.length() + block.text().length() + 1 > properties.getEmbedding().getMaxChunkChars()) {
                flush(chunks, state, sectionPath(sectionStack), chunkIndex, true);
            }
            state.add(block.text(), block.type().name(), boilerplate, block.charStart(), block.charEnd());
            if (state.length() >= properties.getEmbedding().getTargetChunkChars()) {
                flush(chunks, state, sectionPath(sectionStack), chunkIndex, true);
            }
        }
        flush(chunks, state, sectionPath(sectionStack), chunkIndex, false);
        return chunks;
    }

    private void splitLongBlock(
            List<RagChunk> chunks,
            RagDocumentBlock block,
            String sectionPath,
            boolean boilerplate,
            int[] chunkIndex) {
        int max = properties.getEmbedding().getMaxChunkChars();
        int overlap = Math.min(properties.getEmbedding().getOverlapChars(), max / 2);
        int start = 0;
        while (start < block.text().length()) {
            int end = Math.min(block.text().length(), start + max);
            String text = block.text().substring(start, end);
            if (text.length() >= properties.getEmbedding().getMinChunkChars()) {
                chunks.add(new RagChunk(
                        chunkIndex[0]++,
                        text,
                        sectionPath,
                        List.of(block.type().name()),
                        boilerplate,
                        block.charStart() + start,
                        block.charStart() + end));
            }
            if (end >= block.text().length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
    }

    private void flush(
            List<RagChunk> chunks,
            ChunkState state,
            String sectionPath,
            int[] chunkIndex,
            boolean keepOverlap) {
        if (state.length() == 0) {
            return;
        }
        String text = state.text();
        int start = state.charStart;
        int end = state.charEnd;
        Set<String> types = new LinkedHashSet<>(state.blockTypes);
        boolean boilerplate = state.boilerplate;
        if (text.length() >= properties.getEmbedding().getMinChunkChars()) {
            chunks.add(new RagChunk(
                    chunkIndex[0]++,
                    text,
                    sectionPath,
                    new ArrayList<>(types),
                    boilerplate,
                    start,
                    end));
        }
        String overlap = keepOverlap ? overlapText(text) : "";
        state.clear();
        if (!overlap.isBlank()) {
            state.add(overlap, RagBlockType.OVERLAP.name(), boilerplate, Math.max(start, end - overlap.length()), end);
        }
    }

    private String overlapText(String text) {
        int overlapChars = properties.getEmbedding().getOverlapChars();
        if (overlapChars <= 0 || text.length() <= overlapChars) {
            return "";
        }
        int start = text.length() - overlapChars;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '；' || c == ';' || c == '\n') {
                return text.substring(Math.min(i + 1, text.length())).trim();
            }
        }
        return text.substring(start).trim();
    }

    private void updateSection(List<String> sectionStack, int level, String heading) {
        int normalizedLevel = Math.max(1, Math.min(4, level));
        while (sectionStack.size() >= normalizedLevel) {
            sectionStack.remove(sectionStack.size() - 1);
        }
        sectionStack.add(heading);
    }

    private String sectionPath(List<String> sectionStack) {
        return String.join(" > ", sectionStack);
    }

    private boolean isBoilerplate(String text) {
        return BOILERPLATE_KEYWORDS.stream().anyMatch(text::contains);
    }

    private static class ChunkState {
        private final StringBuilder text = new StringBuilder();
        private final List<String> blockTypes = new ArrayList<>();
        private boolean boilerplate;
        private int charStart = -1;
        private int charEnd;

        void add(String value, String blockType, boolean valueBoilerplate, int start, int end) {
            if (text.length() > 0) {
                text.append('\n');
            }
            if (charStart < 0) {
                charStart = start;
            }
            text.append(value);
            blockTypes.add(blockType);
            boilerplate = boilerplate || valueBoilerplate;
            charEnd = end;
        }

        int length() {
            return text.length();
        }

        String text() {
            return text.toString();
        }

        void clear() {
            text.setLength(0);
            blockTypes.clear();
            boilerplate = false;
            charStart = -1;
            charEnd = 0;
        }
    }
}
