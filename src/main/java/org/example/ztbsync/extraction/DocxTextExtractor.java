package org.example.ztbsync.extraction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.StringJoiner;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

@Component
public class DocxTextExtractor {

    public String extract(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringJoiner joiner = new StringJoiner("\n");
            document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(ExtractionTextUtils::hasText)
                    .forEach(joiner::add);
            for (XWPFTable table : document.getTables()) {
                appendTableText(table, joiner);
            }
            return joiner.toString().trim();
        } catch (IOException exception) {
            throw new IllegalArgumentException("DOCX 文件解析失败", exception);
        }
    }

    private void appendTableText(XWPFTable table, StringJoiner joiner) {
        for (XWPFTableRow row : table.getRows()) {
            StringJoiner rowJoiner = new StringJoiner(" ");
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText();
                if (ExtractionTextUtils.hasText(text)) {
                    rowJoiner.add(text.trim());
                }
                for (XWPFTable nestedTable : cell.getTables()) {
                    appendTableText(nestedTable, joiner);
                }
            }
            String rowText = rowJoiner.toString();
            if (ExtractionTextUtils.hasText(rowText)) {
                joiner.add(rowText);
            }
        }
    }
}
