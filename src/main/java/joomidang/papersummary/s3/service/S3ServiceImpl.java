package joomidang.papersummary.s3.service;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 파일 업로드 서비스 구현체
 * <p>
 * 참고: 실제 AWS S3 연동을 위해서는 AWS SDK 의존성 추가 필요 implementation
 * 'org.springframework.cloud:spring-cloud-starter-aws:2.2.6.RELEASE'
 */
@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    // TODO: AWS S3 설정 추가 필요

    @Override
    public String uploadFile(MultipartFile file, String dirName) {
        // 파일명 생성 (UUID 사용)
        String originalFilename = file.getOriginalFilename();
        String filename = UUID.randomUUID() + "_" + originalFilename;

        // TODO: 실제 S3 업로드 로직 구현 필요
        // 현재는 임시 URL 반환
        log.info("파일 업로드: {}", filename);

        // 임시 S3 URL 형식 반환
        return "https://paper-summary-bucket.s3.amazonaws.com/" + dirName + "/" + filename;
    }

    @Override
    public void deleteFile(String fileUrl) {
        // TODO: 실제 S3 파일 삭제 로직 구현 필요
        log.info("파일 삭제: {}", fileUrl);
    }
}