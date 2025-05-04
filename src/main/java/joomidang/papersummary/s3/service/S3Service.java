package joomidang.papersummary.s3.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * S3 파일 업로드 서비스 인터페이스
 */
public interface S3Service {

    /**
     * 파일을 S3에 업로드하고 URL을 반환
     */
    String uploadFile(MultipartFile file, String dirName);

    /**
     * S3에서 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 URL
     */
    void deleteFile(String fileUrl);
}