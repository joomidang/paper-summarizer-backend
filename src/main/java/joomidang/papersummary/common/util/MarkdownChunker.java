package joomidang.papersummary.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 마크다운 문서를 청크로 분할하는 유틸리티 클래스
 */
public class MarkdownChunker {

    /**
     * 마크다운 문서를 헤더 기준으로 청크 분할
     * 헤더(## 또는 ### 등) 기준으로 섹션별 나눔
     * @param markdownContent 마크다운 전체 문자열
     * @param maxTokens 청크별 최대 토큰 수 (512 등)
     * @return 청크 문자열 목록
     */
    public static List<String> chunkBySection(String markdownContent, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        String[] lines = markdownContent.split("\n");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokenCount = 0;

        for (String line : lines) {
            int lineTokens = estimateTokenCount(line);

            // 헤더를 만나면 청크 종료
            if (line.trim().startsWith("#") && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentTokenCount = 0;
            }

            if (currentTokenCount + lineTokens > maxTokens) {
                // 토큰 한도 초과 시 청크 분리
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentTokenCount = 0;
            }

            currentChunk.append(line).append("\n");
            currentTokenCount += lineTokens;
        }

        // 마지막 청크 추가
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 대략적인 토큰 수 추정
     */
    private static int estimateTokenCount(String text) {
        return text.length() / 4; // 영어 기준, 한글은 2.5~3배 높을 수 있음
    }
}