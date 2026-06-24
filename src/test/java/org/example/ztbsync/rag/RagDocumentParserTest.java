package org.example.ztbsync.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

class RagDocumentParserTest {

    @Test
    void extractsParagraphHeadingAndTableRows() throws IOException {
        RagDocumentParser parser = new RagDocumentParser();

        List<RagDocumentBlock> blocks = parser.parse(docxBytes());

        assertThat(blocks).extracting(RagDocumentBlock::type)
                .containsExactly(RagBlockType.HEADING, RagBlockType.PARAGRAPH, RagBlockType.TABLE_ROW);
        assertThat(blocks.get(0).headingLevel()).isEqualTo(2);
        assertThat(blocks.get(2).text()).contains("人员", "张三");
    }

    private byte[] docxBytes() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.createRun().setText("一、项目概况");

            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("本项目需要完成数据治理平台建设。");

            XWPFTable table = document.createTable(1, 2);
            table.getRow(0).getCell(0).setText("人员");
            table.getRow(0).getCell(1).setText("张三");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
