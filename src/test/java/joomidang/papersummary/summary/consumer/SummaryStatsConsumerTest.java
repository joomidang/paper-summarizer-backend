package joomidang.papersummary.summary.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import joomidang.papersummary.common.config.rabbitmq.StatsType;
import joomidang.papersummary.summary.service.SummaryStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SummaryStatsConsumerTest {
    private SummaryStatsConsumer summaryStatsConsumer;
    private SummaryStatsService summaryStatsService;

    @BeforeEach
    void setup() {
        summaryStatsService = mock(SummaryStatsService.class);
        summaryStatsConsumer = new SummaryStatsConsumer(summaryStatsService);
    }

    @Test
    @DisplayName("VIEW 이벤트 처리 테스트")
    void consumeViewEvent() {
        // given
        Long summaryId = 1L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("summaryId", summaryId);
        payload.put("type", StatsType.VIEW.toString());
        // when
        summaryStatsConsumer.consume(payload);
        // then
        verify(summaryStatsService, times(1)).increaseViewCount(summaryId);
    }
}