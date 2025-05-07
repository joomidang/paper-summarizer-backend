package joomidang.papersummary.s3.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import joomidang.papersummary.paper.exception.FileUploadFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 파일 업로드 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String uploadBucketName;

    @Value("${aws.s3.summary-bucket-name}")
    private String summaryBucketName;

    @Override
    public String uploadFile(MultipartFile file, String dirName) {
        log.info("S3 파일 업로드 시작: 파일명={}, 디렉토리={}", file.getOriginalFilename(), dirName);

        // 파일명 생성 (UUID 사용)
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String key = dirName + "/" + filename;
        log.debug("생성된 S3 키: {}", key);

        try {
            log.debug("S3 PutObjectRequest 생성: bucket={}, key={}, contentType={}",
                    uploadBucketName, key, file.getContentType());

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(uploadBucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            log.debug("S3 putObject 요청 실행: fileSize={}", file.getSize());
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = "https://" + uploadBucketName + ".s3.amazonaws.com/" + key;
            log.info("S3 파일 업로드 완료: fileUrl={}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: 파일명={}, 오류={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadFailedException(e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("S3 파일 삭제 시작: fileUrl={}", fileUrl);

        try {
            String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            log.debug("추출된 S3 키: {}", key);

            log.debug("S3 DeleteObjectRequest 생성: bucket={}, key={}", uploadBucketName, key);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(uploadBucketName)
                    .key(key)
                    .build();

            log.debug("S3 deleteObject 요청 실행");
            s3Client.deleteObject(deleteRequest);

            log.info("S3 파일 삭제 완료: key={}", key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: fileUrl={}, 오류={}", fileUrl, e.getMessage(), e);
            throw new FileUploadFailedException("파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public String saveMarkdownToS3(String key, String markdownContent) {
        log.info("S3 마크다운 텍스트 업로드 시작: ");

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(summaryBucketName)
                    .key(key)
                    .contentType("text/markdown")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(markdownContent.getBytes(StandardCharsets.UTF_8)));
            String fileUrl = "https://" + summaryBucketName + ".s3.amazonaws.com/" + key;
            log.info("S3 마크다운 업로드 완료: fileUrl={}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("S3 마크다운 업로드 실패: key={}, 오류={}", key, e.getMessage(), e);
            throw new FileUploadFailedException("마크다운 업로드 실패: " + e.getMessage());
        }
    }
}
