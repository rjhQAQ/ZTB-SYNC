package org.example.ztbsync.extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class BidRegexExtractor {

    private static final List<String> COMPANY_KEYS = List.of("投标公司名称", "投标人名称", "投标人", "投标单位", "供应商名称");
    private static final List<String> PHONE_KEYS = List.of("投标人联系电话", "联系电话", "联系人电话", "电话", "手机");
    private static final List<String> REGISTERED_ADDRESS_KEYS = List.of("注册地址", "注册地");
    private static final List<String> MAILING_ADDRESS_KEYS = List.of("通信地址", "通讯地址", "联系地址");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d{9}|0\\d{2,3}[- ]?\\d{7,8})");
    private static final Pattern PERSON_PATTERN = Pattern.compile("(项目经理|项目负责人|技术负责人|安全员|质量员|施工员|资料员)\\s*[:：]?\\s*([\\u4e00-\\u9fa5]{2,6})?");

    public BidExtraction extract(String text) {
        BidExtraction extraction = new BidExtraction();
        extraction.setBidCompanyName(findField(text, COMPANY_KEYS));
        extraction.setBidderContactPhone(findPhone(text));
        extraction.setRegisteredAddress(findField(text, REGISTERED_ADDRESS_KEYS));
        extraction.setMailingAddress(findField(text, MAILING_ADDRESS_KEYS));
        extraction.setProjectManagementPersonnel(findProjectManagementPersonnel(text));
        return extraction;
    }

    private String findField(String text, List<String> keys) {
        for (String line : ExtractionTextUtils.lines(text)) {
            for (String key : keys) {
                int index = line.indexOf(key);
                if (index >= 0) {
                    String tail = line.substring(index + key.length());
                    if (tail.matches("^\\s*[:：].+")) {
                        return ExtractionTextUtils.cleanValue(tail);
                    }
                }
            }
        }
        return null;
    }

    private String findPhone(String text) {
        for (String line : ExtractionTextUtils.lines(text)) {
            boolean hasPhoneKey = PHONE_KEYS.stream().anyMatch(line::contains);
            if (hasPhoneKey) {
                Matcher matcher = PHONE_PATTERN.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        Matcher matcher = PHONE_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<ProjectManagementPerson> findProjectManagementPersonnel(String text) {
        List<ProjectManagementPerson> personnel = new ArrayList<>();
        for (String line : ExtractionTextUtils.lines(text)) {
            Matcher matcher = PERSON_PATTERN.matcher(line);
            while (matcher.find()) {
                String role = matcher.group(1);
                String name = ExtractionTextUtils.cleanValue(matcher.group(2));
                String phone = findPhone(line);
                String certificate = findCertificate(line);
                personnel.add(new ProjectManagementPerson(name, role, certificate, phone, line));
            }
        }
        return personnel;
    }

    private String findCertificate(String line) {
        Matcher matcher = Pattern.compile("(?:证书编号|证号|证书|编号)\\s*[:：]?\\s*([A-Za-z0-9\\-]+)").matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }
}
