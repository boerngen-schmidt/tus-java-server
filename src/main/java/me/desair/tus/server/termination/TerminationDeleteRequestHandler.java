package me.desair.tus.server.termination;

import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.AbstractRequestHandler;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * When receiving a DELETE request for an existing upload the Server SHOULD free associated resources
 * and MUST respond with the 204 No Content status confirming that the upload was terminated. For all future requests
 * to this URL the Server SHOULD respond with the 404 Not Found or 410 Gone status.
 */
public class TerminationDeleteRequestHandler extends AbstractRequestHandler {

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.DELETE.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        Optional<UploadInfo> uploadInfoOptional = uploadStorageService.getUploadInfo(servletRequest.getRequestURI(), ownerKey);

        if (uploadInfoOptional.isPresent()) {
            uploadStorageService.terminateUpload(uploadInfoOptional.get());
        }

        servletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

}
