package joomidang.papersummary.visualcontent.service;

import java.util.List;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.visualcontent.entity.VisualContent;
import joomidang.papersummary.visualcontent.entity.VisualContentType;
import joomidang.papersummary.visualcontent.repository.VisualContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VisualContentService {
    private final VisualContentRepository visualContentRepository;

    public void saveAll(Paper paper, List<String> urls, VisualContentType type) {
        log.info("시각 콘텐츠 저장 시작: paperId={}, type={}, urlCount={}", 
                paper.getId(), type, urls.size());

        try {
            int position = 0;

            log.debug("시각 콘텐츠 저장 반복 시작: urlCount={}", urls.size());
            for (String url : urls) {
                log.debug("시각 콘텐츠 엔티티 생성: position={}, url={}", position, url);
                VisualContent content = VisualContent.builder()
                        .paper(paper)
                        .storageUrl(url)
                        .type(type)
                        .position(position++)
                        .build();

                log.debug("시각 콘텐츠 저장: position={}", position-1);
                visualContentRepository.save(content);
            }

            log.info("시각 콘텐츠 저장 완료: paperId={}, type={}, 저장된 콘텐츠 수={}", 
                    paper.getId(), type, position);
        } catch (Exception e) {
            log.error("시각 콘텐츠 저장 중 오류 발생: paperId={}, type={}, 오류={}", 
                    paper.getId(), type, e.getMessage(), e);
            throw e;
        }
    }
}
