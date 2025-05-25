package joomidang.papersummary.member.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 이미지 업로드 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResponse {
    private String imageUrl;

    /**
     * 이미지 URL로부터 응답 객체 생성
     *
     * @param imageUrl 업로드된 이미지 URL
     * @return ProfileImageResponse 객체
     */
    public static ProfileImageResponse from(String imageUrl) {
        return new ProfileImageResponse(imageUrl);
    }
}