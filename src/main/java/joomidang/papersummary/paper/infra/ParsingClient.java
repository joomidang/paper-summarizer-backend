package joomidang.papersummary.paper.infra;

public interface ParsingClient {
    void requestParsing(Long paperId, Long userid, String url);
}
