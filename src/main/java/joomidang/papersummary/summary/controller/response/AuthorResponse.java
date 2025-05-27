package joomidang.papersummary.summary.controller.response;

import joomidang.papersummary.member.entity.Member;
import lombok.Builder;

@Builder
public record AuthorResponse(Long id,
                            String username,
                            String profileImageUrl) {
    public static AuthorResponse from(Member member) {
        return AuthorResponse.builder()
                .id(member.getId())
                .username(member.getName())
                .profileImageUrl(member.getProfileImage())
                .build();
    }
}