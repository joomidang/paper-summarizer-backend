package joomidang.papersummary.comment.controller.response;

public record CommentLikeResponse(boolean liked, int likeCount) {
    public static CommentLikeResponse from(String action, int likeCount) {
        if (action == null ||
                (!action.equalsIgnoreCase("like") && !action.equalsIgnoreCase("dislike"))) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        boolean liked = action.equalsIgnoreCase("like");
        return new CommentLikeResponse(liked, likeCount);
    }
}
