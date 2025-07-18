package joomidang.papersummary.paper.controller.request;

import java.util.List;

public record ParsingResultRequest(
        String title,
        String markdownUrl,
        String contentListUrl,
        List<String> figures,
        List<String> tables
) {
}
