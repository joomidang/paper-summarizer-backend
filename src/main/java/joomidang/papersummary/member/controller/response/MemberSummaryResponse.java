package joomidang.papersummary.member.controller.response;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.summary.entity.Summary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryResponse {
    private List<MemberSummaryItemResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static MemberSummaryResponse from(Page<Summary> summaryPage) {
        List<MemberSummaryItemResponse> content = summaryPage.getContent().stream()
                .map(MemberSummaryItemResponse::from)
                .collect(Collectors.toList());

        return MemberSummaryResponse.builder()
                .content(content)
                .page(summaryPage.getNumber() + 1) // Page is 0-based, but we want 1-based for the response
                .size(summaryPage.getSize())
                .totalElements(summaryPage.getTotalElements())
                .totalPages(summaryPage.getTotalPages())
                .build();
    }
}