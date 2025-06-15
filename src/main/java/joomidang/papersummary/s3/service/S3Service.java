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

    /**
     * 마크다운을 S3에 업로드
     */
    String saveMarkdownToS3(String key, String markdownContent);

    /**
     * 프로필 이미지를 처리하고 S3에 업로드
     * Thumbnailator를 사용하여 이미지 크기를 조정하고 최적화
     * 
     * @param file 업로드할 프로필 이미지 파일
     * @return 업로드된 이미지의 URL
     */
    String uploadProfileImage(MultipartFile file);
}
