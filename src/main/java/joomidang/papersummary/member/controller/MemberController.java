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
import joomidang.papersummary.member.controller.response.MemberInterestResponse;
import joomidang.papersummary.member.controller.response.MemberSuccessCode;
import joomidang.papersummary.member.controller.response.CreateProfileResponse;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import joomidang.papersummary.paper.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Tag(name = "Users", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;

    /**
     * 회원가입 이후 프로필 생성
     * @param request
     * @return
     */
    @Operation(summary = "사용자 프로필 등록", description = "사용자가 회원가입 단계에서 가입후 프로필 정보를 입력하는 API(닉네임, 관심분야 등)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "프로필 등록 성공", responseCode = "200")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description = "인증 실패",
            responseCode = "403",
            content = @Content(examples = @ExampleObject(value = "{\"code\": \"AUTH-0001\", \"message\": \"인증에 실패했습니다.\"}"))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description = "잘못된 입력형태",
            responseCode = "400",
            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MEM-0002\", \"message\": \"관심분야는 최소 1개 이상 입력해야 합니다.\"}"))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description = "닉네임 중복",
            responseCode = "409",
            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MEM-0003\", \"message\": \"이미 사용 중인 닉네임입니다.\"}"))
    )

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<CreateProfileResponse>> createProfile(
            @Valid @RequestBody ProfileCreateRequest request, Authentication authentication, HttpServletRequest httpRequest) {

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
//    public ResponseEntity<ApiResponse<Void>> getMyProfile(
//            @RequestBody HttpServletRequest request, Authentication authentication
//    ){
//        return
//    }
}