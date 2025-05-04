package joomidang.papersummary.paper.service;

import java.time.LocalDateTime;
import joomidang.papersummary.analysislog.entity.AnalysisLog;
import joomidang.papersummary.analysislog.entity.AnalysisSourceType;
import joomidang.papersummary.analysislog.entity.AnalysisStage;
import joomidang.papersummary.analysislog.entity.AnalysisStatus;
import joomidang.papersummary.analysislog.repository.AnalysisLogRepository;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.entity.Paper;
import joomidang.papersummary.paper.entity.Status;
import joomidang.papersummary.paper.exception.InvalidFileTypeException;
import joomidang.papersummary.paper.exception.PaperNotFoundException;
import joomidang.papersummary.paper.repository.PaperRepository;
import joomidang.papersummary.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 논문 관련 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaperService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String S3_PAPERS_FOLDER = "papers";

    private final S3Service s3Service;
    private final MemberService memberService;
    private final PaperRepository paperRepository;
    private final AnalysisLogRepository analysisLogRepository;

    /**
     * PDF 파일을 업로드하고 Paper 엔티티 생성
     *
     * @param file        업로드할 PDF 파일
     * @param providerUid 사용자 식별자
     * @return 저장된 Paper 엔티티
     * @throws InvalidFileTypeException PDF가 아닌 파일이 업로드된 경우
     */
    @Transactional
    public Paper uploadPaper(MultipartFile file, String providerUid) {
        log.info("논문 업로드 시작: {}, 사용자: {}", file.getOriginalFilename(), providerUid);

        // 사용자 확인
        Member member = findMember(providerUid);

        // 파일 타입 검증
        validateFileType(file);

        // S3에 파일 업로드
        String s3Url = uploadFileToS3(file);

        // Paper 엔티티 생성 및 저장
        Paper savedPaper = createAndSavePaper(file, s3Url, member);

        // 분석 로그 생성
        createAnalysisLog(savedPaper, member);

        log.info("논문 업로드 완료: {}, ID: {}", file.getOriginalFilename(), savedPaper.getId());
        return savedPaper;
    }

    public Paper findById(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException(paperId));
    }

    /**
     * 사용자 정보 조회
     */
    private Member findMember(String providerUid) {
        log.info("사용자 확인 중: {}", providerUid);
        return memberService.findByProviderUid(providerUid);
    }

    /**
     * 파일 타입 검증 (PDF만 허용)
     */
    private void validateFileType(MultipartFile file) {
        log.info("파일 타입 검증 중: {}", file.getOriginalFilename());
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(PDF_CONTENT_TYPE)) {
            log.warn("잘못된 파일 형식: {}, 타입: {}", file.getOriginalFilename(), contentType);
            throw new InvalidFileTypeException();
        }
    }

    /**
     * S3에 파일 업로드
     */
    private String uploadFileToS3(MultipartFile file) {
        log.info("S3에 업로드 중: {}", file.getOriginalFilename());
        return s3Service.uploadFile(file, S3_PAPERS_FOLDER);
    }

    /**
     * Paper 엔티티 생성 및 저장
     */
    private Paper createAndSavePaper(MultipartFile file, String s3Url, Member member) {
        log.info("Paper 엔티티 생성 및 저장 중: {}", file.getOriginalFilename());
        Paper paper = Paper.builder()
                .title(null) // 메타데이터 없이 저장
                .filePath(s3Url)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .status(Status.PENDING)
                .member(member)
                .build();

        return paperRepository.save(paper);
    }

    /**
     * 분석 로그 생성 및 저장
     */
    private void createAnalysisLog(Paper paper, Member member) {
        log.info("분석 로그 엔티티 생성 및 저장 중: {}", paper.getId());
        AnalysisLog analysisLog = AnalysisLog.builder()
                .paper(paper)
                .member(member)
                .status(AnalysisStatus.PENDING)
                .startedAt(LocalDateTime.now())
                .stage(AnalysisStage.MINERU)
                .sourceType(AnalysisSourceType.UPLOAD)
                .build();

        analysisLogRepository.save(analysisLog);
    }
}
