package joomidang.papersummary.summary.controller.response;

public record SummaryLikeResponse(boolean liked, int likeCount) {
    public static SummaryLikeResponse from(String action, int likeCount) {
        if (action == null ||
                (!action.equalsIgnoreCase("like") && !action.equalsIgnoreCase("unlike"))) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        boolean liked = "like".equalsIgnoreCase(action);
        return new SummaryLikeResponse(liked, likeCount);
    }
}
