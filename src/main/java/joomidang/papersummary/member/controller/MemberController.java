package joomidang.papersummary.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import joomidang.papersummary.common.controller.response.ApiResponse;
import joomidang.papersummary.member.controller.request.ProfileCreateRequest;
import joomidang.papersummary.member.controller.response.MemberSuccessCode;
import joomidang.papersummary.member.controller.response.ProfileResponse;
import joomidang.papersummary.member.entity.Member;
import joomidang.papersummary.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Users", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "사용자 프로필 등록", description = "사용자가 회원가입 단계에서 가입후 프로필 정보를 입력하는 API(닉네임, 관심분야 등)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "프로필 등록 성공", responseCode = "200")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "잘못된 입력형태", responseCode = "400")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "인증 실패", responseCode = "403")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "닉네임 중복", responseCode = "409")
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(@Valid @RequestBody ProfileCreateRequest request) {
        Long memberId = request.getId();
        log.info("프로필 생성 요청: userId={}, request={}", memberId, request);

        // 프로필 생성
        Member member = memberService.createProfile(memberId, request);
        
        // 응답 생성
        ProfileResponse response = ProfileResponse.from(member);
        
        return ResponseEntity.ok(ApiResponse.successWithData(MemberSuccessCode.PROFILE_CREATED, response));
    }
}