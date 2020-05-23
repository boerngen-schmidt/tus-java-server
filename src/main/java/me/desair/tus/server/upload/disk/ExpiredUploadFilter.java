package me.desair.tus.server.upload.disk;

import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadLockingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Directory stream filter that only accepts uploads that are still in progress and expired
 */
public class ExpiredUploadFilter implements DirectoryStream.Filter<Path> {

    private static final Logger log = LoggerFactory.getLogger(ExpiredUploadFilter.class);

    private DiskStorageService diskStorageService;
    private UploadLockingService uploadLockingService;

    ExpiredUploadFilter(DiskStorageService diskStorageService, UploadLockingService uploadLockingService) {
        this.diskStorageService = diskStorageService;
        this.uploadLockingService = uploadLockingService;
    }

    @Override
    public boolean accept(Path upload) throws IOException {
        UploadId id = null;
        try {
            id = new UploadId(upload.getFileName().toString());
            Optional<UploadInfo> info = diskStorageService.getUploadInfo(id);

            if (info.isPresent() && info.get().isExpired() && !uploadLockingService.isLocked(id)) {
                return true;
            }

        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("Not able to determine state of upload " + Objects.toString(id), ex);
            }
        }

        return false;
    }
}
