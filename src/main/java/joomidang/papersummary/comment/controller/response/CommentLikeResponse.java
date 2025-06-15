package joomidang.papersummary.comment.controller.response;

import joomidang.papersummary.comment.exception.InvalidLikeActionException;

public record CommentLikeResponse(boolean liked, int likeCount) {
    public static CommentLikeResponse from(String action, int likeCount) {
        if (action == null ||
                (!action.equalsIgnoreCase("like") && !action.equalsIgnoreCase("dislike"))) {
            throw new InvalidLikeActionException(action);
        }
        boolean liked = action.equalsIgnoreCase("like");
        return new CommentLikeResponse(liked, likeCount);
    }
}
