package joomidang.papersummary.summary.service;

import java.util.Optional;
import joomidang.papersummary.common.config.rabbitmq.StatsEventPublisher;
import joomidang.papersummary.common.config.rabbitmq.StatsType;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.summary.controller.response.LikedSummaryListResponse;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryLike;
import joomidang.papersummary.summary.repository.SummaryLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SummaryLikeService {
    private final SummaryLikeRepository summaryLikeRepository;
    private final MemberService memberService;
    private final StatsEventPublisher statsEventPublisher;


    @Transactional
    public boolean toggleLike(String providerUid, Summary summary) {
        log.debug("좋아요 토글 시작: summaryId={}, providerUid={}", summary.getId(), providerUid);

        Member member = memberService.findByProviderUid(providerUid);

        // 기존 좋아요 상태 확인
        Optional<SummaryLike> existingLike = summaryLikeRepository.findByMemberAndSummary(member,
                summary);

        if (existingLike.isPresent()) {
            // 이미 좋아요가 있으면 제거 (좋아요 취소)
            summaryLikeRepository.delete(existingLike.get());
            statsEventPublisher.publish(summary.getId(), StatsType.DISLIKE);
            log.debug("좋아요 취소 완료: summaryId={}, memberId={}", summary.getId(), member.getId());
            return false;
        } else {
            // 좋아요가 없으면 추가
            SummaryLike summaryLike = SummaryLike.of(member, summary);
            summaryLikeRepository.save(summaryLike);
            statsEventPublisher.publish(summary.getId(), StatsType.LIKE);
            log.debug("좋아요 추가 완료: summaryId={}, memberId={}", summary.getId(), member.getId());
            return true;
        }
    }

    /**
     * 좋아요한 요약본 목록 조회
     */
    public LikedSummaryListResponse getLikedSummaries(String providerUid, Pageable pageable) {
        log.debug("좋아요한 요약본 목록 조회 시작: providerUid={}", providerUid);

        Member member = memberService.findByProviderUid(providerUid);
        Page<SummaryLike> summaryLikes = summaryLikeRepository.findByMemberIdWithSummary(
                member.getId(),
                PublishStatus.PUBLISHED,
                pageable
        );

        log.debug("좋아요한 요약본 목록 조회 완료: providerUid={}, count={}",
                providerUid, summaryLikes.getTotalElements());

        return LikedSummaryListResponse.from(summaryLikes);
    }
}
