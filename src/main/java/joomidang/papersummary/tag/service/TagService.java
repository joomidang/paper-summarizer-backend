package joomidang.papersummary.tag.service;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.tag.entity.SummaryTag;
import joomidang.papersummary.tag.entity.Tag;
import joomidang.papersummary.tag.repository.SummaryTagRepository;
import joomidang.papersummary.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}