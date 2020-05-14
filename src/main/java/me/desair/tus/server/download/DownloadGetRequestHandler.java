package me.desair.tus.server.download;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadInProgressException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.AbstractRequestHandler;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Send the uploaded bytes of finished uploads
 */
public class DownloadGetRequestHandler extends AbstractRequestHandler {

    private static final String CONTENT_DISPOSITION_FORMAT = "attachment;filename=\"%s\"";

    @Override
    public boolean supports(HttpMethod method) {
        return HttpMethod.GET.equals(method);
    }

    @Override
    public void process(HttpMethod method, TusServletRequest servletRequest,
                        TusServletResponse servletResponse, UploadStorageService uploadStorageService,
                        String ownerKey) throws IOException, TusException {

        Optional<UploadInfo> uploadInfoOptional = uploadStorageService.getUploadInfo(servletRequest.getRequestURI(), ownerKey);
        UploadInfo uploadInfo = uploadInfoOptional.filter(i -> !i.isUploadInProgress())
                .orElseThrow(() -> new UploadInProgressException("Upload " + servletRequest.getRequestURI() + " is still in progress "
                        + "and cannot be downloaded yet"));

        // We got an uploadInfo and Upload is not in progress
        servletResponse.setHeader(HttpHeader.CONTENT_LENGTH, Objects.toString(uploadInfo.getLength()));

        servletResponse.setHeader(HttpHeader.CONTENT_DISPOSITION,
                String.format(CONTENT_DISPOSITION_FORMAT, uploadInfo.getFileName()));

        servletResponse.setHeader(HttpHeader.CONTENT_TYPE, uploadInfo.getFileMimeType());

        if (uploadInfo.hasMetadata()) {
            servletResponse.setHeader(HttpHeader.UPLOAD_METADATA, uploadInfo.getEncodedMetadata());
        }

        uploadStorageService.copyUploadTo(uploadInfo, servletResponse.getOutputStream());

        servletResponse.setStatus(HttpServletResponse.SC_OK);
    }
}
