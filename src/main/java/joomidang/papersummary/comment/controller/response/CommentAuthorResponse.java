package joomidang.papersummary.comment.controller.response;

import joomidang.papersummary.member.entity.Member;
import lombok.Builder;

@Builder
public record CommentAuthorResponse(Long id,
                                    String name,
                                    String profileImage) {
    public static CommentAuthorResponse from(Member member) {
        return CommentAuthorResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .profileImage(member.getProfileImage())
                .build();
    }
}
