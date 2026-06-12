package org.example.ztbsync.extraction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TimeNormalizer {

    public static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(
            "(\\d{4})\\s*(?:年|[-/.])\\s*(\\d{1,2})\\s*(?:月|[-/.])\\s*(\\d{1,2})\\s*日?"
                    + "(?:\\s*(上午|下午|晚上|中午|早上)?\\s*(\\d{1,2})(?:\\s*(?::|：|时)\\s*(\\d{1,2}))?\\s*(?:分)?)?");

    private static final List<DateTimeFormatter> FALLBACK_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
            DateTimeFormatter.ofPattern("yyyy.M.d H:mm"),
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm"));

    public List<TimePoint> extractTimePoints(String text) {
        List<TimePoint> points = new ArrayList<>();
        for (String line : ExtractionTextUtils.lines(text)) {
            Matcher matcher = DATE_TIME_PATTERN.matcher(line);
            while (matcher.find()) {
                Optional<LocalDateTime> normalized = parseMatch(matcher);
                if (normalized.isPresent()) {
                    points.add(new TimePoint(extractLabel(line, matcher.start()), matcher.group(),
                            format(normalized.get()), "REGEX"));
                }
            }
        }
        return deduplicate(points);
    }

    public Optional<LocalDateTime> parse(String value) {
        if (!ExtractionTextUtils.hasText(value)) {
            return Optional.empty();
        }
        String normalized = value.trim()
                .replace('T', ' ')
                .replaceAll("\\s+", " ");
        Matcher matcher = DATE_TIME_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return parseMatch(matcher);
        }
        for (DateTimeFormatter formatter : FALLBACK_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(normalized, formatter));
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        try {
            return Optional.of(LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    public String format(LocalDateTime time) {
        return time == null ? null : OUTPUT_FORMATTER.format(time);
    }

    public Optional<LocalDateTime> earliest(List<TimePoint> points) {
        return points.stream()
                .map(TimePoint::normalizedTime)
                .map(this::parse)
                .flatMap(Optional::stream)
                .min(Comparator.naturalOrder());
    }

    public Optional<LocalDateTime> latest(List<TimePoint> points) {
        return points.stream()
                .map(TimePoint::normalizedTime)
                .map(this::parse)
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder());
    }

    private Optional<LocalDateTime> parseMatch(Matcher matcher) {
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            int hour = matcher.group(5) == null ? 0 : Integer.parseInt(matcher.group(5));
            int minute = matcher.group(6) == null ? 0 : Integer.parseInt(matcher.group(6));
            String meridiem = matcher.group(4);
            if (("下午".equals(meridiem) || "晚上".equals(meridiem)) && hour < 12) {
                hour += 12;
            }
            if ("中午".equals(meridiem) && hour < 11) {
                hour += 12;
            }
            return Optional.of(LocalDateTime.of(year, month, day, hour, minute));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String extractLabel(String line, int matchStart) {
        String prefix = line.substring(0, Math.max(0, matchStart)).trim();
        if (prefix.length() > 40) {
            prefix = prefix.substring(prefix.length() - 40);
        }
        prefix = prefix.replaceAll("^[,，。；;、\\s]+", "").replaceAll("[：:\\s]+$", "");
        return prefix.isBlank() ? "时间点" : prefix;
    }

    private List<TimePoint> deduplicate(List<TimePoint> points) {
        List<TimePoint> deduplicated = new ArrayList<>();
        for (TimePoint point : points) {
            boolean exists = deduplicated.stream().anyMatch(existing ->
                    existing.normalizedTime().equals(point.normalizedTime())
                            && existing.label().equals(point.label()));
            if (!exists) {
                deduplicated.add(point);
            }
        }
        return deduplicated;
    }
}
