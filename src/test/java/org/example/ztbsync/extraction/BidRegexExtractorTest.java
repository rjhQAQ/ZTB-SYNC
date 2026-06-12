package org.example.ztbsync.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BidRegexExtractorTest {

    private final BidRegexExtractor extractor = new BidRegexExtractor();

    @Test
    void extractsBidCompanyContactAddressAndPersonnel() {
        String text = """
                投标人名称：杭州示例投标有限公司
                投标人联系电话：13800138000
                注册地址：浙江省杭州市西湖区示例路1号
                通信地址：浙江省杭州市滨江区示例街2号
                项目经理：张三 证书编号 ABC-123 电话 13900139000
                技术负责人：李四
                """;

        BidExtraction result = extractor.extract(text);

        assertThat(result.getBidCompanyName()).isEqualTo("杭州示例投标有限公司");
        assertThat(result.getBidderContactPhone()).isEqualTo("13800138000");
        assertThat(result.getRegisteredAddress()).isEqualTo("浙江省杭州市西湖区示例路1号");
        assertThat(result.getMailingAddress()).isEqualTo("浙江省杭州市滨江区示例街2号");
        assertThat(result.getProjectManagementPersonnel())
                .extracting(ProjectManagementPerson::role)
                .containsExactly("项目经理", "技术负责人");
    }
}
