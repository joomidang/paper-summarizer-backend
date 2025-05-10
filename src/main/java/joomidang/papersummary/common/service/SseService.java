package joomidang.papersummary.common.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseService {
    // 타임아웃 설정 (1시간)
    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;

    // 이벤트 타입 상수
    private static final String EVENT_CONNECT = "connect";
    private static final String EVENT_SUMMARY_COMPLETED = "summary_completed";
    private static final String EVENT_PARSING_COMPLETED = "parsing_completed";

    private static final String MSG_CONNECTED = "연결되었습니다.";
    private static final String MSG_SUMMARY_COMPLETED = "분석이 완료되었습니다.";
    private static final String MSG_PARSING_COMPLETED = "논문 파싱이 완료되었습니다.";

    // 클라이언트 연결을 저장하는 맵 (paperId -> SseEmitter)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 SSE 연결 생성
     */
    public SseEmitter createConnection(Long paperId) {
        log.info("SSE 연결 생성 → paperId={}", paperId);
        removeExistingConnection(paperId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        configureEmitterCallbacks(emitter, paperId);

        // 초기 연결 유지 이벤트 전송
        if (!sendInitialEvent(emitter, paperId)) {
            return emitter; // 초기 이벤트 전송 실패 시 early return
        }

        // 맵에 저장
        emitters.put(paperId, emitter);
        return emitter;
    }

    /**
     * 파싱 완료 이벤트 전송
     */
    public boolean sendParsingCompletedEvent(Long paperId) {
        log.info("파싱 완료 이벤트 전송 → paperId={}", paperId);

        Map<String, Object> eventData = Map.of(
                "message", MSG_PARSING_COMPLETED,
                "paperId", paperId
        );

        // 파싱 완료 후에는 연결을 종료하지 않고 유지 (요약 완료 이벤트를 위해)
        return sendEvent(paperId, EVENT_PARSING_COMPLETED, eventData);
    }

    /**
     * 요약 완료 이벤트 전송
     */
    public boolean sendSummaryCompletedEvent(Long paperId, Long summaryId) {
        log.info("요약 완료 이벤트 전송 → paperId={}, summaryId={}", paperId, summaryId);

        Map<String, Object> eventData = Map.of(
                "message", MSG_SUMMARY_COMPLETED,
                "summaryId", summaryId
        );

        boolean success = sendEvent(paperId, EVENT_SUMMARY_COMPLETED, eventData);

        // 요약 완료 후 연결 종료
        if (success) {
            completeEmitter(paperId);
        }

        return success;
    }

    /**
     * 기존 연결이 있으면 제거
     */
    private void removeExistingConnection(Long paperId) {
        if (emitters.containsKey(paperId)) {
            log.debug("기존 SSE 연결 제거 → paperId={}", paperId);
            SseEmitter oldEmitter = emitters.get(paperId);
            oldEmitter.complete();
            emitters.remove(paperId);
        }
    }

    /**
     * SSE 이미터 콜백 설정
     */
    private void configureEmitterCallbacks(SseEmitter emitter, Long paperId) {
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료 → paperId={}", paperId);
            emitters.remove(paperId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃 → paperId={}", paperId);
            emitter.complete();
            emitters.remove(paperId);
        });

        emitter.onError((e) -> {
            log.error("SSE 연결 에러 → paperId={}, error={}", paperId, e.getMessage());
            emitter.complete();
            emitters.remove(paperId);
        });
    }

    /**
     * 초기 연결 이벤트 전송
     */
    private boolean sendInitialEvent(SseEmitter emitter, Long paperId) {
        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_CONNECT)
                    .data(MSG_CONNECTED));
            log.debug("SSE 초기 이벤트 전송 성공 → paperId={}", paperId);
            return true;
        } catch (IOException e) {
            log.error("SSE 초기 이벤트 전송 실패 → paperId={}, error={}", paperId, e.getMessage());
            emitter.complete();
            return false;
        }
    }

    /**
     * 이벤트 전송 공통 메서드
     */
    private boolean sendEvent(Long paperId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(paperId);
        if (emitter == null) {
            log.warn("SSE 연결을 찾을 수 없음 → paperId={}", paperId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.debug("SSE 이벤트 전송 성공 → paperId={}, eventName={}", paperId, eventName);
            return true;
        } catch (IOException e) {
            log.error("SSE 이벤트 전송 실패 → paperId={}, eventName={}, error={}",
                    paperId, eventName, e.getMessage());
            emitter.complete();
            emitters.remove(paperId);
            return false;
        }
    }

    /**
     * SSE 연결 완료 처리
     */
    private void completeEmitter(Long paperId) {
        SseEmitter emitter = emitters.get(paperId);
        if (emitter != null) {
            log.debug("SSE 연결 완료 처리 → paperId={}", paperId);
            emitter.complete();
            // onCompletion 콜백에서 map에서 제거됨
        }
    }
}