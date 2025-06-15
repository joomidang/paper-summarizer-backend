package joomidang.docs.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.OperationResponse;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;

public class UriDecodingPreprocessor implements OperationPreprocessor {
    @Override
    public OperationRequest preprocess(OperationRequest request) {
        final String decodeUri = URLDecoder.decode(request.getUri().toString(), StandardCharsets.UTF_8);
        return new OperationRequestFactory().create(
                URI.create(decodeUri),
                request.getMethod(),
                request.getContent(),
                request.getHeaders(),
                request.getParts(),
                request.getCookies()
        );
    }

    @Override
    public OperationResponse preprocess(OperationResponse operationResponse) {
        return null;
    }
}
