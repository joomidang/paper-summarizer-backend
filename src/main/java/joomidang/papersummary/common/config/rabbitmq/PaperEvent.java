package joomidang.papersummary.common.config.rabbitmq;

import java.io.Serializable;

public record PaperEvent(
        PaperEventType type,
        Long paperId
) implements Serializable {
}