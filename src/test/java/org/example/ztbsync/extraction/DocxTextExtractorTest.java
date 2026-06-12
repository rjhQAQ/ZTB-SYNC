package org.example.ztbsync.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

class DocxTextExtractorTest {

    @Test
    void extractsParagraphAndTableText() throws Exception {
        byte[] bytes;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("项目名称：测试项目");
            XWPFTable table = document.createTable(1, 2);
            table.getRow(0).getCell(0).setText("招标人");
            table.getRow(0).getCell(1).setText("示例公司");
            document.write(output);
            bytes = output.toByteArray();
        }

        String text = new DocxTextExtractor().extract(bytes);

        assertThat(text).contains("项目名称：测试项目");
        assertThat(text).contains("招标人 示例公司");
    }
}
