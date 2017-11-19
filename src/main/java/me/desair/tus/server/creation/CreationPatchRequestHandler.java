package me.desair.tus.server.creation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.RequestHandler;
import me.desair.tus.server.upload.UploadIdFactory;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.TusServletResponse;
import me.desair.tus.server.util.Utils;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Upload-Defer-Length: 1 if upload size is not known at the time. Once it is known the Client MUST set
 * the Upload-Length header in the next PATCH request. Once set the length MUST NOT be changed.
 */
public class CreationPatchRequestHandler implements RequestHandler {

    @Override
    public boolean supports(final HttpMethod method) {
        return HttpMethod.HEAD.equals(method);
    }

    @Override
    public void process(final HttpMethod method, final HttpServletRequest servletRequest, final TusServletResponse servletResponse, final UploadStorageService uploadStorageService, final UploadIdFactory idFactory) {
        UUID id = idFactory.readUploadId(servletRequest);
        UploadInfo uploadInfo = uploadStorageService.getUploadInfo(id);

        if(!uploadInfo.hasLength()) {
            Long uploadLength = Utils.getLongHeader(servletRequest, HttpHeader.UPLOAD_LENGTH);
            if(uploadLength != null) {
                uploadInfo.setLength(uploadLength);
                uploadStorageService.update(uploadInfo);
            }
        }
    }
}
