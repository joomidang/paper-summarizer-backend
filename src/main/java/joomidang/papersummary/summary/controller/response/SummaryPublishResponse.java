package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;

public record SummaryPublishResponse(
        Long summaryId,
        String markdownUrl,
        LocalDateTime publishedAt
) {
    public static SummaryPublishResponse of(Long id, String url, LocalDateTime at) {
        return new SummaryPublishResponse(id, url, at);
    }
}
