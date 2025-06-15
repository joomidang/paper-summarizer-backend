package joomidang.papersummary.common.config.rabbitmq;

import java.io.Serializable;

public record PaperEventEnvelop<T>(
        PaperEventType type,
        T payload
) implements Serializable {
}
