package me.desair.tus.server.checksum;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.AbstractExtensionRequestHandler;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Tus-Checksum-Algorithm header MUST be included in the response to an OPTIONS request.
 * The Tus-Checksum-Algorithm response header MUST be a comma-separated list of the checksum
 * algorithms supported by the server.
 */
public class ChecksumOptionsRequestHandler extends AbstractExtensionRequestHandler {

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) {

        super.process(method, servletRequest, servletResponse, uploadStorageService, ownerKey);
        String checksumAlgorithms = Stream.of(ChecksumAlgorithm.values()).map(ChecksumAlgorithm::getTusName).collect(Collectors.joining(","));
        servletResponse.setHeader(HttpHeader.TUS_CHECKSUM_ALGORITHM, checksumAlgorithms);
    }

    @Override
    protected void appendExtensions(StringBuilder extensionBuilder) {
        addExtension(extensionBuilder, "checksum");
        addExtension(extensionBuilder, "checksum-trailer");
    }

}
