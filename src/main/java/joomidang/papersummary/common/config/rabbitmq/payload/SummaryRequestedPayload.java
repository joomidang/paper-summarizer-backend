package joomidang.papersummary.common.config.rabbitmq.payload;

public record SummaryRequestedPayload(
        Long paperId,
        String markdownUrl,
        String contentListUrl,
        String prompt,
        String language
) {
}
