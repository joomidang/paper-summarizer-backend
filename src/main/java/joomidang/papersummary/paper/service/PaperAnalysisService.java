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
        log.info("논문 분석 요청 처리 시작: paperId={}, providerUid={}", paperId, providerUid);

        try {
            log.debug("논문 정보 조회 시작: paperId={}", paperId);
            Paper paper = paperRepository.findById(paperId)
                    .orElseThrow(() -> {
                        log.error("논문 정보 조회 실패: paperId={} 존재하지 않음", paperId);
                        return new PaperNotFoundException(paperId);
                    });
            log.debug("논문 정보 조회 완료: paperId={}, filePath={}", paperId, paper.getFilePath());

            log.debug("사용자 정보 조회 시작: providerUid={}", providerUid);
            Member requester = memberService.findByProviderUid(providerUid);
            log.debug("사용자 정보 조회 완료: memberId={}", requester.getId());

            log.debug("논문 접근 권한 검증: paperId={}, memberId={}", paperId, requester.getId());
            if (paper.hasNotPermission(requester.getId())) {
                log.error("논문 접근 권한 없음: paperId={}, memberId={}", paperId, requester.getId());
                throw new AccessDeniedException();
            }
            log.debug("논문 접근 권한 검증 완료");

            //TODO: 추후에 prompt, language redis 저장해서 api 요청 보낼때 활용
            log.debug("분석 요청 이벤트 페이로드 생성: paperId={}, memberId={}, filePath={}", 
                    paper.getId(), requester.getId(), paper.getFilePath());
            ParsingRequestedPayload payload = new ParsingRequestedPayload(
                    paper.getId(),
                    requester.getId(),
                    paper.getFilePath()
            );

            log.info("PARSING_REQUESTED 이벤트 발행: paperId={}, prompt={}, language={}", 
                    paperId, prompt, language);
            paperEventPublisher.publish(new PaperEventEnvelop<>(PaperEventType.PARSING_REQUESTED, payload));

            log.info("논문 분석 요청 처리 완료: paperId={}", paperId);
        } catch (PaperNotFoundException | AccessDeniedException e) {
            // 이미 로그가 기록된 예외는 다시 던짐
            throw e;
        } catch (Exception e) {
            log.error("논문 분석 요청 처리 중 예상치 못한 오류 발생: paperId={}, 오류={}", 
                    paperId, e.getMessage(), e);
            throw e;
        }
    }
}
