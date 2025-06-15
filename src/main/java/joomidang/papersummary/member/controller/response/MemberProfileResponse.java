package joomidang.papersummary.member.controller.response;

import joomidang.papersummary.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String[] interests;
//    private Long followerCount;
//    private Long followingCount;
    private Long summaryCount;
    private Long likedSummaryCount;
    private Long commentCount;

    public static MemberProfileResponse from(Member member, String[] interests, Long summaryCount, Long likedSummaryCount, Long commentCount) {
        return MemberProfileResponse.builder()
                .id(member.getId())
                .username(member.getName())
                .email(member.getEmail())
                .profileImageUrl(member.getProfileImage())
                .createdAt(member.getCreatedAt())
                .lastLoginAt(member.getLastLoginAt())
                .interests(interests)
                .summaryCount(summaryCount)
                .likedSummaryCount(likedSummaryCount)
                .commentCount(commentCount)
                .build();
    }
}