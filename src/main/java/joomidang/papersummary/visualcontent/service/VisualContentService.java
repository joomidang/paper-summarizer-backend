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
        int position = 0;

        for (String url : urls) {
            VisualContent content = VisualContent.builder()
                    .paper(paper)
                    .storageUrl(url)
                    .type(type)
                    .position(position++)
                    .build();
            visualContentRepository.save(content);
        }
    }
}
