package me.desair.tus.server.core.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.RequestValidator;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadOffsetMismatchException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * The Upload-Offset header’s value MUST be equal to the current offset of the resource.
 * If the offsets do not match, the Server MUST respond with the
 * 409 Conflict status without modifying the upload resource.
 */
public class UploadOffsetValidator implements RequestValidator {

    @Override
    public void validate(HttpMethod method, HttpServletRequest request,
                         UploadStorageService uploadStorageService, String ownerKey)
            throws IOException, TusException {

        String uploadOffset = Utils.getHeader(request, HttpHeader.UPLOAD_OFFSET);

        Optional<UploadInfo> uploadInfoOptional = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);

        if (uploadInfoOptional.isPresent()) {
            UploadInfo uploadInfo = uploadInfoOptional.get();
            String expectedOffset = Objects.toString(uploadInfo.getOffset());
            if (!StringUtils.equals(expectedOffset, uploadOffset)) {
                throw new UploadOffsetMismatchException("The Upload-Offset was "
                        + StringUtils.trimToNull(uploadOffset) + " but expected " + expectedOffset);
            }
        }

    }

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.PATCH.equals(method);
    }

}
