package joomidang.papersummary.s3.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import joomidang.papersummary.paper.exception.FileUploadFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 파일 업로드 서비스 가짜 구현체
 * <p>
 * 로컬 환경에서 테스트를 위해 사용되며, 실제 S3에 업로드하지 않고 메모리에 저장한다.
 */
@Slf4j
@Service
@Profile("local")
public class FakeS3Service implements S3Service {

    private static final String FAKE_S3_BASE_URL = "https://paper-dev-test-magic-pdf-output.s3.bucket.com/";
    private final Map<String, String> fakeS3Storage = new HashMap<>();

    @Override
    public String uploadFile(MultipartFile file, String dirName) {
        log.info("가짜 S3 파일 업로드 시작: 파일명={}, 디렉토리={}", file.getOriginalFilename(), dirName);

        try {
            // 파일명 생성 (UUID 사용)
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String key = dirName + "/" + filename;
            
            // 파일 내용을 문자열로 변환 (실제로는 바이너리 파일도 처리해야 하지만 테스트용이므로 간단히 구현)
            String content = new String(file.getBytes());
            fakeS3Storage.put(key, content);
            
            String fileUrl = FAKE_S3_BASE_URL + key;
            log.info("가짜 S3 파일 업로드 완료: fileUrl={}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("가짜 S3 파일 업로드 실패: 파일명={}, 오류={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadFailedException(e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("가짜 S3 파일 삭제 시작: fileUrl={}", fileUrl);

        try {
            String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            fakeS3Storage.remove(key);
            log.info("가짜 S3 파일 삭제 완료: key={}", key);
        } catch (Exception e) {
            log.error("가짜 S3 파일 삭제 실패: fileUrl={}, 오류={}", fileUrl, e.getMessage(), e);
            throw new FileUploadFailedException("파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public String saveMarkdownToS3(String key, String markdownContent) {
        log.info("가짜 S3 마크다운 텍스트 업로드 시작: key={}", key);

        try {
            // 가짜 스토리지에 마크다운 내용 저장
            fakeS3Storage.put(key, markdownContent);
            
            String fileUrl = FAKE_S3_BASE_URL + key;
            log.info("가짜 S3 마크다운 업로드 완료: fileUrl={}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("가짜 S3 마크다운 업로드 실패: key={}, 오류={}", key, e.getMessage(), e);
            throw new FileUploadFailedException("마크다운 업로드 실패: " + e.getMessage());
        }
    }
    
    /**
     * 가짜 S3 스토리지에서 마크다운 내용 조회
     * <p>
     * 실제 S3Service에는 없는 메서드이지만, 테스트를 위해 추가
     */
    public String getMarkdownContent(String key) {
        return fakeS3Storage.getOrDefault(key, "# 가짜 요약 내용\n\n이 내용은 테스트를 위한 가짜 요약 내용입니다.");
    }
}