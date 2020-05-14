package me.desair.tus.server.concatenation.validation;

import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.RequestValidator;
import me.desair.tus.server.exception.PatchOnFinalUploadNotAllowedException;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.upload.UploadType;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * The Server MUST respond with the 403 Forbidden status to PATCH requests against a upload URL
 * and MUST NOT modify the or its partial uploads.
 */
public class PatchFinalUploadValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        Optional<UploadInfo> uploadInfo = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);

        if (uploadInfo.isPresent() && UploadType.CONCATENATED.equals(uploadInfo.get().getUploadType())) {
            throw new PatchOnFinalUploadNotAllowedException("You cannot send a PATCH request for a "
                    + "concatenated upload URI");
        }
    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }
}
