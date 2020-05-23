package me.desair.tus.server.core.validation;

import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.RequestValidator;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadNotFoundException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * If the resource is not found, the Server SHOULD return either the
 * 404 Not Found, 410 Gone or 403 Forbidden status without the Upload-Offset header.
 */
public class IdExistsValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws TusException, IOException {

        Optional<UploadInfo> uploadInfo = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);
        uploadInfo.orElseThrow(() -> new UploadNotFoundException("The upload for path " + request.getRequestURI()
                + " and owner " + ownerKey + " was not found."));
    }

    @Override
    public boolean supports(HttpMethod method) {
        return method != null && (
                HttpMethod.HEAD.equals(method)
                        || HttpMethod.PATCH.equals(method)
                        || HttpMethod.DELETE.equals(method)
                        || HttpMethod.GET.equals(method)
            );
    }

}
