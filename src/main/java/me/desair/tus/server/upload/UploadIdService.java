package me.desair.tus.server.upload;

import java.util.Optional;

public interface UploadIdService {
    UploadId newUploadId();

    /**
     * Extract Id from URL
     * @param url
     * @return
     */
    Optional<UploadId> fromRequestUri(String url);

}
