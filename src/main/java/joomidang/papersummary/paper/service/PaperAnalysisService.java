package joomidang.papersummary.paper.service;

import joomidang.papersummary.common.config.rabbitmq.PaperEventEnvelop;
import joomidang.papersummary.common.config.rabbitmq.PaperEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.PaperEventType;
import joomidang.papersummary.common.config.rabbitmq.payload.ParsingRequestedPayload;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.paper.exception.PaperNotFoundException;
import joomidang.papersummary.paper.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 논문 분석 요청 처리 서비스
 * <p>
 * 사용자가 논문 분석을 요청하면: 1. 사용자 권한을 검증하고 2. 논문 정보를 조회한 뒤 3. 파싱 요청 이벤트(PARSING_REQUESTED)를 발행한다.
 * <p>
 * 이 이벤트는 RabbitMQ를 통해 Consumer에 전달되고, 실제 파싱 서버 호출은 비동기적으로 이루어진다.
 * <p>
 * 주요 목적: 논문 분석 트리거 역할 - 외부 파싱 서버와 직접 통신하지 않음 - 분석 로직은 PaperParsingConsumer에서 처리됨
 */

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaperAnalysisService {
    private final MemberService memberService;
    private final PaperRepository paperRepository;
    private final PaperEventPublisher paperEventPublisher;

    @Transactional
    public void requestParsing(Long paperId, String providerUid, String prompt, String language) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException(paperId));

        Member requester = memberService.findByProviderUid(providerUid);

        if (paper.hasNotPermission(requester.getId())) {
            throw new AccessDeniedException();
        }

        //TODO: 추후에 prompt, language 저장해서 api 요청 보낼때 활용
        log.info("PARSING_REQUESTED 이벤트 발행 -> paperId={}, prompt={}", paperId, prompt);
        ParsingRequestedPayload payload = new ParsingRequestedPayload(
                paper.getId(),
                requester.getId(),
                paper.getFilePath()
        );
        paperEventPublisher.publish(new PaperEventEnvelop<>(PaperEventType.PARSING_REQUESTED, payload));
    }
}
