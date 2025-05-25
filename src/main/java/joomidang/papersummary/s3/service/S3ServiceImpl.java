package joomidang.papersummary.s3.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import joomidang.papersummary.paper.exception.FileUploadFailedException;
import joomidang.papersummary.paper.exception.InvalidFileTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
@Profile("!local")
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
                    .contentType("text/plain; charset=UTF-8")
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

    @Override
    public String uploadProfileImage(MultipartFile file) {
        log.info("프로필 이미지 업로드 시작: 파일명={}, 크기={}KB", file.getOriginalFilename(), file.getSize() / 1024);

        if (file.isEmpty()) {
            log.error("업로드 실패: 파일이 비어있습니다.");
            throw new InvalidFileTypeException("업로드할 파일이 비어있습니다.");
        }

        // 이미지 파일 확장자 확인
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 지원하는 이미지 형식인지 확인
        if (!isImageFile(fileExtension)) {
            log.error("업로드 실패: 지원하지 않는 파일 형식입니다. 확장자={}", fileExtension);
            throw new InvalidFileTypeException("지원하지 않는 이미지 형식입니다. JPG, JPEG, PNG, GIF 파일만 업로드 가능합니다.");
        }

        try {
            // 이미지 처리 (Thumbnailator 사용)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(300, 300)  // 프로필 이미지 크기 조정
                    .keepAspectRatio(true)  // 종횡비 유지
                    .outputQuality(0.8)  // 이미지 품질 설정 (80%)
                    .toOutputStream(outputStream);

            byte[] resizedImageBytes = outputStream.toByteArray();
            log.debug("이미지 처리 완료: 원본 크기={}KB, 처리 후 크기={}KB", 
                    file.getSize() / 1024, resizedImageBytes.length / 1024);

            // 파일명 생성 (UUID 사용)
            String filename = UUID.randomUUID() + fileExtension;
            String key = "profiles/" + filename;  // 프로필 이미지는 profiles 디렉토리에 저장

            // S3에 업로드
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(uploadBucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(
                    new ByteArrayInputStream(resizedImageBytes), resizedImageBytes.length));

            String fileUrl = "https://" + uploadBucketName + ".s3.amazonaws.com/" + key;
            log.info("프로필 이미지 업로드 완료: fileUrl={}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("프로필 이미지 업로드 실패: 파일명={}, 오류={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadFailedException("프로필 이미지 업로드 실패: " + e.getMessage());
        }
    }

    /**
     * 지원하는 이미지 파일 형식인지 확인
     */
    private boolean isImageFile(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return false;
        }

        String ext = fileExtension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif");
    }
}
