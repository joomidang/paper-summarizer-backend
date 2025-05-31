package joomidang.papersummary.summary.controller.response;

import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public record SummaryListResponse(
        List<SummaryResponse> summaries,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {
    public static SummaryListResponse from(Page<SummaryResponse> summariesPage) {
        return new SummaryListResponse(
                summariesPage.getContent(),
                summariesPage.getNumber(),
                summariesPage.getTotalPages(),
                summariesPage.getTotalElements(),
                summariesPage.hasNext(),
                summariesPage.hasPrevious()
        );
    }

    public static SummaryListResponse empty(Pageable pageable) {
        Page<SummaryResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                pageable,
                0
        );
        return SummaryListResponse.from(emptyPage);
    }
}
