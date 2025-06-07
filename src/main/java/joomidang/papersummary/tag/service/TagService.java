package joomidang.papersummary.tag.service;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.tag.entity.SummaryTag;
import joomidang.papersummary.tag.entity.Tag;
import joomidang.papersummary.tag.repository.SummaryTagRepository;
import joomidang.papersummary.tag.repository.TagRepository;
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
public class TagService {
    private final TagRepository tagRepository;
    private final SummaryTagRepository summaryTagRepository;

    /**
     * 요약본에 태그들을 연결
     */
    @Transactional
    public void attachTagsToSummary(Summary summary, List<String> tagNames) {
        log.debug("요약본에 태그 연결 시작: summaryId={}, tagNames={}", summary.getId(), tagNames);
        if (tagNames == null || tagNames.isEmpty()) {
            log.debug("연결할 태그가 없음");
            return;
        }

        summaryTagRepository.deleteBySummary(summary);

        //새로운 태그 관계 생성
        for (String tagName : tagNames) {
            if (!Tag.isValidTagName(tagName)) {
                log.warn("유효하지 않은 태그명 무시: {}", tagName);
                continue;
            }

            Tag tag = findOrCreateTag(tagName);

            // 중복 방지
            if (!summaryTagRepository.existsBySummaryAndTag(summary, tag)) {
                SummaryTag summaryTag = SummaryTag.of(summary, tag);
                summaryTagRepository.save(summaryTag);
                tag.increaseUsageCount();
                tagRepository.save(tag);

                log.debug("태그 연결 완료: summaryId={}, tagId={}, tagName={}",
                        summary.getId(), tag.getId(), tag.getName());
            }
        }
        log.debug("요약본 태그 연결 완료: summaryId={}", summary.getId());
    }

    /**
     * 태그 찾기 또는 생성
     */
    @Transactional
    protected Tag findOrCreateTag(String tagName) {
        String normalizedName = tagName.trim().toLowerCase().replaceAll("\\s+", " ");

        return tagRepository.findByName(normalizedName)
                .orElseGet(() -> {
                    log.debug("새 태그 생성: {}", normalizedName);
                    Tag newTag = Tag.create(normalizedName);
                    return tagRepository.save(newTag);
                });
    }

    /**
     * 요약본의 태그 목록 조회
     */
    public List<String> getTagNamesBySummary(Long summaryId) {
        log.debug("요약본 태그 목록 조회: summaryId={}", summaryId);

        List<SummaryTag> summaryTags = summaryTagRepository.findBySummaryIdWithTag(summaryId);
        List<String> tagNames = summaryTags.stream()
                .map(st -> st.getTag().getName())
                .collect(Collectors.toList());

        log.debug("요약본 태그 목록 조회 완료: summaryId={}, tagCount={}", summaryId, tagNames.size());
        return tagNames;
    }

    /**
     * 요약본 삭제 시 태그 사용 횟수 감소
     */
    @Transactional
    public void decreaseTagUsageForSummary(Summary summary) {
        log.debug("요약본 삭제로 인한 태그 사용 횟수 감소: summaryId={}", summary.getId());

        List<SummaryTag> summaryTags = summaryTagRepository.findBySummaryIdWithTag(summary.getId());

        for (SummaryTag summaryTag : summaryTags) {
            Tag tag = summaryTag.getTag();
            tag.decreaseUsageCount();
            tagRepository.save(tag);

            log.debug("태그 사용 횟수 감소: tagId={}, tagName={}, newCount={}",
                    tag.getId(), tag.getName(), tag.getUsageCount());
        }

        // 관계 삭제
        summaryTagRepository.deleteBySummary(summary);

        log.debug("요약본 태그 관계 삭제 완료: summaryId={}", summary.getId());
    }

    /**
     * 특정 태그를 가진 요약본 목록 조회 (페이징, 정렬 지원)
     */
    public Page<Summary> getSummariesByTag(String tagName, Pageable pageable) {
        log.debug("태그별 요약본 목록 조회 시작: tagName={}, page={}, size={}",
                tagName, pageable.getPageNumber(), pageable.getPageSize());

        // 태그명 정규화
        String normalizedTagName = tagName.trim().toLowerCase().replaceAll("\\s+", " ");

        // 발행된 요약본만 조회
        Page<Summary> summaries = summaryTagRepository.findSummariesByTagName(
                normalizedTagName, PublishStatus.PUBLISHED, pageable);

        log.debug("태그별 요약본 목록 조회 완료: tagName={}, 조회된 요약본 수={}",
                tagName, summaries.getContent().size());

        return summaries;
    }
}