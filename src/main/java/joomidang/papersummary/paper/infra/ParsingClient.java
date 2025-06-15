package joomidang.papersummary.paper.infra;

import joomidang.papersummary.common.config.rabbitmq.payload.ParsingRequestedPayload;

public interface ParsingClient {
    void requestParsing(ParsingRequestedPayload payload);
}
