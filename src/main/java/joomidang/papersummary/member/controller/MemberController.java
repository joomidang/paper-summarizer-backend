package joomidang.papersummary.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import joomidang.papersummary.auth.resolver.Authenticated;
import joomidang.papersummary.auth.security.JwtTokenProvider;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.response.*;
import joomidang.papersummary.member.controller.request.UpdateProfileRequest;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import joomidang.papersummary.s3.service.S3Service;
import joomidang.papersummary.summary.controller.response.LikedSummaryListResponse;
import joomidang.papersummary.summary.service.SummaryLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Users", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;
    private final S3Service s3Service;
    private final SummaryLikeService summaryLikeService;

    /**
     * 회원가입 이후 프로필 생성
     * @param request
     * @return
     */
    @Operation(summary = "사용자 프로필 등록", description = "사용자가 회원가입 단계에서 가입후 프로필 정보를 입력하는 API(닉네임, 관심분야 등)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "프로필 등록 성공", responseCode = "200")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "인증 실패", responseCode = "403", content = @Content(examples = @ExampleObject(value = "{\"code\": \"AUTH-0001\", \"message\": \"인증에 실패했습니다.\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "잘못된 입력형태", responseCode = "400", content = @Content(examples = @ExampleObject(value = "{\"code\": \"MEM-0002\", \"message\": \"관심분야는 최소 1개 이상 입력해야 합니다.\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "닉네임 중복", responseCode = "409", content = @Content(examples = @ExampleObject(value = "{\"code\": \"MEM-0003\", \"message\": \"이미 사용 중인 닉네임입니다.\"}")))
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<CreateProfileResponse>> createProfile(
            @Valid @RequestBody ProfileCreateRequest request, Authentication authentication, HttpServletRequest httpRequest) {

        // providerUid로 고쳐야 하는데 언제 고치지
        // 토큰에서 사용자 정보 확인
        String token = httpRequest.getHeader("Authorization");
        if (token != null) {
            String userIdFromToken = tokenProvider.getUserId(token);
            String authName = authentication.getName();
            log.info("토큰조회 userId={} name={}", userIdFromToken, authName);
            if (!authName.equals(userIdFromToken)) {
                log.debug("JWT 토큰의 사용자와 인증된 사용자가 일치하지 않습니다. JWT: {}, Auth: {}",
                        userIdFromToken, authName);
                throw new AccessDeniedException("인증 사용자와 토큰 사용자가 일치하지 않습니다");
            }
        }

        String providerUid = authentication.getName();
        Member authenticatedMember = memberService.findByProviderUid(providerUid);
        Long memberId = authenticatedMember.getId();
        log.info("프로필 생성 요청: userId={}, providerUid={} request={}", memberId, providerUid,request);

        // 프로필 생성
        Member member = memberService.createProfile(memberId, request);

        // 응답 생성
        CreateProfileResponse response = CreateProfileResponse.from(member);

        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.PROFILE_CREATED, response));
    }


    /**
     * 사용자 프로필 수정
     *
     * @param request 수정할 프로필 정보 (선택적 필드)
     * @param providerUid 인증 제공자가 제공한 인증된 사용자의 고유 식별자
     * @return 수정된 프로필 정보가 포함된 ApiResponse를 담은 ResponseEntity
     */
    @Operation(summary = "사용자 프로필 수정", description = "인증된 사용자의 프로필 정보(닉네임, 관심분야, 프로필 이미지)를 수정합니다. 모든 필드는 선택적입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "프로필 수정 성공", responseCode = "200")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "인증 실패", responseCode = "403")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "잘못된 입력형태", responseCode = "400")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "닉네임 중복", responseCode = "409")
    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<CreateProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @Parameter(hidden = true)
            @Authenticated String providerUid
    ) {
        log.info("프로필 수정 요청: providerUid={}, request={}", providerUid, request);

        // 프로필 수정
        Member updatedMember = memberService.updateProfile(providerUid, request);

        // 응답 생성
        CreateProfileResponse response = CreateProfileResponse.from(updatedMember);

        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.PROFILE_UPDATED, response));
    }

    /**
     * 인증된 사용자의 관심분야를 조회
     *
     * @param providerUid 인증 제공자가 제공한 인증된 사용자의 고유 식별자
     * @return 관심분야 데이터가 포함된 ApiResponse를 담은 ResponseEntity
     */
    @GetMapping("/me/interests")
    public ResponseEntity<ApiResponse<MemberInterestResponse>> getInterests(
            @Parameter(hidden = true)
            @Authenticated String providerUid
    ){
        log.debug("provierUid={}", providerUid);
        Long memberId = memberService.findByProviderUid(providerUid).getId();
        String[] interests = memberService.getInterests(memberId);
        MemberInterestResponse response = MemberInterestResponse.from(interests);
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.MEMBER_INFO, response));
    }

    /**
     * 인증된 사용자가 작성한 요약 목록을 조회
     *
     * @param providerUid 인증 제공자가 제공한 인증된 사용자의 고유 식별자
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 10)
     * @return 페이지네이션된 요약 목록 데이터가 포함된 ApiResponse를 담은 ResponseEntity
     */
    @Operation(summary = "사용자 작성 요약 목록 조회", description = "인증된 사용자가 작성한 요약 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/me/summaries")
    public ResponseEntity<ApiResponse<MemberSummaryResponse>> getSummaries(
            @Parameter(hidden = true)
            @Authenticated String providerUid,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(required = false, defaultValue = "10") int size
    ){
        log.debug("사용자 요약 목록 조회: providerUid={}, page={}, size={}", providerUid, page, size);
        Long memberId = memberService.findByProviderUid(providerUid).getId();
        MemberSummaryResponse summaries = memberService.getSummaries(memberId, page, size);
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.MEMBER_SUMMARIES, summaries));
    }

    /**
     * 사용자 작성 댓글 목록 조회
     *
     * @param providerUid
     * @param page
     * @param size
     * @return
     */
    @Operation(summary = "사용자 작성 댓글 목록 조회", description = "인증된 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<MemberCommentResponse>> getComments(
            @Parameter(hidden = true)
            @Authenticated String providerUid,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(required = false, defaultValue = "10") int size
    ){
        log.debug("사용자가 작성한 댓글 모두 조회: providerUid={}, page={}, size={}", providerUid, page, size);
        Long memberId = memberService.findByProviderUid(providerUid).getId();
        MemberCommentResponse comments = memberService.getComments(memberId, page, size);
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.MEMBER_COMMENTS, comments));
    }

    /**
     * 사용자 프로필 이미지 업로드
     *
     * @param file 업로드할 프로필 이미지 파일
     * @param providerUid 인증 제공자가 제공한 인증된 사용자의 고유 식별자
     * @return 업로드된 이미지 URL이 포함된 ApiResponse를 담은 ResponseEntity
     */
    @Operation(summary = "사용자 프로필 이미지 업로드", description = "사용자의 프로필 이미지를 업로드합니다. 이미지는 자동으로 리사이징되어 저장됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "이미지 업로드 성공", responseCode = "200")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "인증 실패", responseCode = "403")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "잘못된 파일 형식", responseCode = "400")
    @PostMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageResponse>> uploadProfileImage(
            @Parameter(description = "파일 업로드", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true)
            @Authenticated String providerUid
    ) {
        log.info("프로필 이미지 업로드 요청: providerUid={}, 파일명={}, 크기={}KB", providerUid, file.getOriginalFilename(), file.getSize() / 1024);

        // 이미지 업로드 및 처리
        String imageUrl = s3Service.uploadProfileImage(file);
        log.info("프로필 이미지 업로드 완료: imageUrl={}", imageUrl);

        // 사용자 프로필 이미지 URL 업데이트
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .profileImageUrl(imageUrl)
                .build();
        memberService.updateProfile(providerUid, updateRequest);
        log.info("사용자 프로필 이미지 URL 업데이트 완료: providerUid={}", providerUid);

        // 응답 생성
        ProfileImageResponse response = ProfileImageResponse.from(imageUrl);
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.PROFILE_IMAGE_UPLOADED, response));
    }

    /**
     * 좋아요한 요약 목록 조회
     * @param providerUid
     * @param page
     * @param size
     * @return
     */
    @Operation(summary = "사용자 좋아요한 요약 목록 조회", description = "인증된 사용자가 좋아요한 요약 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/me/likes")
    public ResponseEntity<ApiResponse<LikedSummaryListResponse>> getLikedSummaries(
            @Parameter(hidden = true)
            @Authenticated String providerUid,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(required = false, defaultValue = "10") int size
    ){
        log.info("좋아요한 글 목록 조회: providerUid={}, page={}, size={}", providerUid, page, size);

        if (page > 0) page = page - 1;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 좋아요한 요약 목록 조회
        LikedSummaryListResponse response = summaryLikeService.getLikedSummaries(providerUid, pageable);

        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.MEMBER_LIKED_SUMMARIES, response));
    }

    /**
     * 사용자 본인 프로필 조회
     * @param providerUid
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @Parameter(hidden = true)
            @Authenticated String providerUid
    ){
        log.info("사용자 본인 프로필 조회 요청: providerUid={}", providerUid);
        Long memberId = memberService.findByProviderUid(providerUid).getId();
        MemberProfileResponse response = memberService.getMemberProfile(memberId);
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.MEMBER_INFO, response));
    }

    /**
     * 다른사용자 프로필 조회
     * @param providerUid
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMemberProfile(
            @Parameter(hidden = true)
            @Authenticated String providerUid,
            @PathVariable Long userId
    ){
        log.info("다른 사용자 프로필 조회 요청: providerUid={}", providerUid);
        MemberProfileResponse response = memberService.getMemberProfile(userId);
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.MEMBER_INFO, response));

    }
}
