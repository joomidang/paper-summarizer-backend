package joomidang.papersummary.common.embedding;

import java.util.List;

public interface EmbeddingClient {
    List<Float> embed(String modelId, String input);
}
