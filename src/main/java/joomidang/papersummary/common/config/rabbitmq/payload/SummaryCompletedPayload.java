package joomidang.papersummary.common.config.rabbitmq.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SummaryCompletedPayload(
        @JsonProperty("paperId") Long paperId,
        @JsonProperty("s3Key") String s3Key
) {
}
