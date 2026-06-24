package org.example.ztbsync.rag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

@Component
public class RagDocumentParser {

    private static final Pattern HEADING_STYLE = Pattern.compile("(?i).*heading\\s*(\\d+).*");
    private static final Pattern CHINESE_HEADING = Pattern.compile("^[一二三四五六七八九十]+[、.．]\\S+");
    private static final Pattern NUMBER_HEADING = Pattern.compile("^\\d+(?:\\.\\d+)*[、.．\\s]+\\S+");
    private static final Pattern CHAPTER_HEADING = Pattern.compile("^第[一二三四五六七八九十0-9]+[章节条]\\S*");
    private static final Pattern LIST_ITEM = Pattern.compile("^(?:\\(?[一二三四五六七八九十0-9]+\\)?[、.．]|[（(][一二三四五六七八九十0-9]+[）)]|[-•●])\\s*\\S+");

    public List<RagDocumentBlock> parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("DOCX 文件内容为空");
        }
        List<RagDocumentBlock> blocks = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            int[] cursor = new int[] {0};
            int[] index = new int[] {0};
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraph(blocks, paragraph, index, cursor);
                } else if (element instanceof XWPFTable table) {
                    appendTable(blocks, table, index, cursor);
                }
            }
            return blocks;
        } catch (IOException exception) {
            throw new IllegalArgumentException("DOCX 文件解析失败", exception);
        }
    }

    private void appendParagraph(
            List<RagDocumentBlock> blocks,
            XWPFParagraph paragraph,
            int[] index,
            int[] cursor) {
        String text = clean(paragraph.getText());
        if (text.isBlank()) {
            return;
        }
        int headingLevel = headingLevel(paragraph, text);
        RagBlockType type = headingLevel > 0
                ? RagBlockType.HEADING
                : (LIST_ITEM.matcher(text).matches() ? RagBlockType.LIST_ITEM : RagBlockType.PARAGRAPH);
        appendBlock(blocks, index, cursor, type, text, headingLevel);
    }

    private void appendTable(List<RagDocumentBlock> blocks, XWPFTable table, int[] index, int[] cursor) {
        for (XWPFTableRow row : table.getRows()) {
            StringJoiner joiner = new StringJoiner(" | ");
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = clean(cell.getText());
                if (!text.isBlank()) {
                    joiner.add(text);
                }
                for (XWPFTable nestedTable : cell.getTables()) {
                    appendTable(blocks, nestedTable, index, cursor);
                }
            }
            String rowText = joiner.toString();
            if (!rowText.isBlank()) {
                appendBlock(blocks, index, cursor, RagBlockType.TABLE_ROW, rowText, 0);
            }
        }
    }

    private void appendBlock(
            List<RagDocumentBlock> blocks,
            int[] index,
            int[] cursor,
            RagBlockType type,
            String text,
            int headingLevel) {
        int start = cursor[0];
        int end = start + text.length();
        blocks.add(new RagDocumentBlock(index[0]++, type, text, headingLevel, start, end));
        cursor[0] = end + 1;
    }

    private int headingLevel(XWPFParagraph paragraph, String text) {
        String style = paragraph.getStyle();
        if (style != null) {
            Matcher matcher = HEADING_STYLE.matcher(style);
            if (matcher.matches()) {
                return clampLevel(parseInt(matcher.group(1), 1));
            }
        }
        if (text.length() > 80) {
            return 0;
        }
        if (CHAPTER_HEADING.matcher(text).matches()) {
            return 1;
        }
        if (CHINESE_HEADING.matcher(text).matches()) {
            return 2;
        }
        if (NUMBER_HEADING.matcher(text).matches()) {
            String prefix = text.split("[、.．\\s]+", 2)[0];
            return clampLevel(prefix.split("\\.").length);
        }
        return 0;
    }

    private int clampLevel(int value) {
        return Math.max(1, Math.min(4, value));
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String clean(String value) {
        return value == null
                ? ""
                : value.replace('\u00a0', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();
    }
}
