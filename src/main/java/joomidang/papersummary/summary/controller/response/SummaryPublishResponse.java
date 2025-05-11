package joomidang.papersummary.summary.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SummaryPublishResponse(
        Long summaryId,
        String title,
        String markdownUrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime publishedAt
) {
    public static SummaryPublishResponse of(Long id, String title, String url, LocalDateTime at) {
        return new SummaryPublishResponse(id, title, url, at);
    }
}
