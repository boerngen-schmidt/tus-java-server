package me.desair.tus.server;

import me.desair.tus.server.checksum.ChecksumExtension;
import me.desair.tus.server.concatenation.ConcatenationExtension;
import me.desair.tus.server.core.CoreProtocol;
import me.desair.tus.server.creation.CreationExtension;
import me.desair.tus.server.download.DownloadExtension;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.expiration.ExpirationExtension;
import me.desair.tus.server.termination.TerminationExtension;
import me.desair.tus.server.upload.*;
import me.desair.tus.server.upload.cache.ThreadLocalCachedStorageAndLockingService;
import me.desair.tus.server.upload.disk.DiskLockingService;
import me.desair.tus.server.upload.disk.DiskStorageService;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Helper class that implements the server side tus v1.0.0 upload protocol
 */
public class TusFileUploadService {

    public static final String TUS_API_VERSION = "1.0.0";

    private static final Logger log = LoggerFactory.getLogger(TusFileUploadService.class);

    private UploadStorageService uploadStorageService;
    private UploadLockingService uploadLockingService;
    private final LinkedHashMap<String, TusExtension> enabledFeatures = new LinkedHashMap<>();
    private final Set<HttpMethod> supportedHttpMethods = EnumSet.noneOf(HttpMethod.class);
    private boolean isThreadLocalCacheEnabled = false;
    private boolean isChunkedTransferDecodingEnabled = false;
    private UploadIdService uploadIdService;
    private String endpoint;

    private TusFileUploadService() {}

    /**
     * Enable or disable a thread-local based cache of upload data. This can reduce the load
     * on the storage backends. By default this cache is disabled.
     * @param isEnabled True if the cache should be enabled, false otherwise
     * @return The builder
     * @deprecated
     */
    public TusFileUploadService withThreadLocalCache(boolean isEnabled) {
        this.isThreadLocalCacheEnabled = isEnabled;
        return this;
    }

    /**
     * Add a custom (application-specific) extension that implements the {@link me.desair.tus.server.TusExtension}
     * interface. For example you can add your own extension that checks authentication and authorization policies
     * within your application for the user doing the upload.
     *
     * @param feature The custom extension implementation
     */
    public void addTusExtension(TusExtension feature) {
        Validate.notNull(feature, "A custom feature cannot be null");
        enabledFeatures.put(feature.getName(), feature);
        updateSupportedHttpMethods();
    }

    /**
     * Disable the TusExtension for which the getName() method matches the provided string. The default extensions
     * have names "creation", "checksum", "expiration", "concatenation", "termination" and "download".
     * You cannot disable the "core" feature.
     *
     * @param extensionName The name of the extension to disable
     */
    public void disableTusExtension(String extensionName) {
        Validate.notNull(extensionName, "The extension name cannot be null");
        Validate.isTrue(!StringUtils.equals("core", extensionName), "The core protocol cannot be disabled");

        enabledFeatures.remove(extensionName);
        updateSupportedHttpMethods();
    }

    /**
     * Get all HTTP methods that are supported by this TusUploadService based on the enabled and/or disabled
     * tus extensions
     *
     * @return The set of enabled HTTP methods
     */
    public Set<HttpMethod> getSupportedHttpMethods() {
        return EnumSet.copyOf(supportedHttpMethods);
    }

    /**
     * Get the set of enabled Tus extensions
     * @return The set of active extensions
     */
    public Set<String> getEnabledFeatures() {
        return new LinkedHashSet<>(enabledFeatures.keySet());
    }

    /**
     * Process a tus upload request.
     * Use this method to process any request made to the main and sub tus upload endpoints. This corresponds to
     * the path specified in the withUploadURI() method and any sub-path of that URI.
     *
     * @param servletRequest The {@link HttpServletRequest} of the request
     * @param servletResponse The {@link HttpServletResponse} of the request
     * @throws IOException When saving bytes or information of this requests fails
     */
    public void process(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws IOException {
        process(servletRequest, servletResponse, (String) null);
    }

    /**
     * Process a tus upload request that belongs to a specific owner.
     * Use this method to process any request made to the main and sub tus upload endpoints. This corresponds to
     * the path specified in the withUploadURI() method and any sub-path of that URI.
     *
     * @param servletRequest The {@link HttpServletRequest} of the request
     * @param servletResponse The {@link HttpServletResponse} of the request
     * @param ownerKey A unique identifier of the owner (group) of this upload
     * @throws IOException When saving bytes or information of this requests fails
     */
    public void process(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                        String ownerKey) throws IOException {
        Validate.notNull(servletRequest, "The HTTP Servlet request cannot be null");
        Validate.notNull(servletResponse, "The HTTP Servlet response cannot be null");

        HttpMethod method = HttpMethod.getMethodIfSupported(servletRequest, supportedHttpMethods);

        log.debug("Processing request with method {} and URL {}", method, servletRequest.getRequestURL());

        TusServletRequest request = new TusServletRequest(servletRequest, isChunkedTransferDecodingEnabled);
        TusServletResponse response = new TusServletResponse(servletResponse);

        try (UploadLock lock = uploadLockingService.lockUploadByUri(request.getRequestURI())) {

            processLockedRequest(method, request, response, ownerKey);

        } catch (TusException e) {
            log.error("Unable to lock upload for request URI " + request.getRequestURI(), e);
        }
    }

    /**
     *
     * @param request
     * @param response
     * @param id Already by user extracted {@link UploadId} by the outer environment
     */
    public void process(HttpServletRequest request, HttpServletResponse response, UploadId id) {

    }

    /**
     * Method to retrieve the bytes that were uploaded to a specific upload URI
     *
     * @param uploadURI The URI of the upload
     * @return An {@link InputStream} that will stream the uploaded bytes
     * @throws IOException  When the retreiving the uploaded bytes fails
     * @throws TusException When the upload is still in progress or cannot be found
     */
    public InputStream getUploadedBytes(String uploadURI) throws IOException, TusException {
        return getUploadedBytes(uploadURI, null);
    }

    /**
     * Method to retrieve the bytes that were uploaded to a specific upload URI
     *
     * @param uploadURI The URI of the upload
     * @param ownerKey  The key of the owner of this upload
     * @return An {@link InputStream} that will stream the uploaded bytes
     * @throws IOException  When the retreiving the uploaded bytes fails
     * @throws TusException When the upload is still in progress or cannot be found
     */
    public InputStream getUploadedBytes(String uploadURI, String ownerKey)
            throws IOException, TusException {

        try (UploadLock lock = uploadLockingService.lockUploadByUri(uploadURI)) {

            return uploadStorageService.getUploadedBytes(uploadURI, ownerKey);
        }
    }

    /**
     * Get the information on the upload corresponding to the given upload URI
     *
     * @param uploadURI The URI of the upload
     * @return Information on the upload
     * @throws IOException  When retrieving the upload information fails
     * @throws TusException When the upload is still in progress or cannot be found
     */
    public Optional<UploadInfo> getUploadInfo(String uploadURI) throws IOException, TusException {
        return getUploadInfo(uploadURI, null);
    }

    /**
     * Get the information on the upload corresponding to the given upload URI
     *
     * @param uploadURI The URI of the upload
     * @param ownerKey  The key of the owner of this upload
     * @return Information on the upload
     * @throws IOException  When retrieving the upload information fails
     * @throws TusException When the upload is still in progress or cannot be found
     */
    public Optional<UploadInfo> getUploadInfo(String uploadURI, String ownerKey) throws IOException, TusException {
        try (UploadLock lock = uploadLockingService.lockUploadByUri(uploadURI)) {

            return uploadStorageService.getUploadInfo(uploadURI, ownerKey);
        }
    }

    /**
     * Method to delete an upload associated with the given upload URL. Invoke this method if you no longer need
     * the upload.
     *
     * @param uploadURI The upload URI
     */
    public void deleteUpload(String uploadURI) throws IOException, TusException {
        deleteUpload(uploadURI, null);
    }

    /**
     * Method to delete an upload associated with the given upload URL. Invoke this method if you no longer need
     * the upload.
     *
     * @param uploadURI The upload URI
     * @param ownerKey  The key of the owner of this upload
     */
    public void deleteUpload(String uploadURI, String ownerKey) throws IOException, TusException {
        try (UploadLock lock = uploadLockingService.lockUploadByUri(uploadURI)) {
            Optional<UploadInfo> uploadInfo = uploadStorageService.getUploadInfo(uploadURI, ownerKey);
            if (uploadInfo.isPresent()) {
                uploadStorageService.terminateUpload(uploadInfo.get());
            }
        }
    }

    /**
     * This method should be invoked periodically. It will cleanup any expired uploads
     * and stale locks
     *
     * @throws IOException When cleaning fails
     */
    public void cleanup() throws IOException {
        uploadLockingService.cleanupStaleLocks();
        uploadStorageService.cleanupExpiredUploads(uploadLockingService);
    }

    protected void processLockedRequest(HttpMethod method, TusServletRequest request,
                                        TusServletResponse response, String ownerKey) throws IOException {
        try {
            validateRequest(method, request, ownerKey);

            executeProcessingByFeatures(method, request, response, ownerKey);

        } catch (TusException e) {
            processTusException(method, request, response, ownerKey, e);
        }
    }

    protected void executeProcessingByFeatures(HttpMethod method, TusServletRequest servletRequest,
                                               TusServletResponse servletResponse, String ownerKey)
            throws IOException, TusException {

        for (TusExtension feature : enabledFeatures.values()) {
            if (!servletRequest.isProcessedBy(feature)) {
                servletRequest.addProcessor(feature);
                feature.process(method, servletRequest, servletResponse, uploadStorageService, ownerKey);
            }
        }
    }

    protected void validateRequest(HttpMethod method, HttpServletRequest servletRequest,
                                   String ownerKey) throws TusException, IOException {

        for (TusExtension feature : enabledFeatures.values()) {
            feature.validate(method, servletRequest, uploadStorageService, ownerKey);
        }
    }

    protected void processTusException(HttpMethod method, TusServletRequest request,
                                       TusServletResponse response, String ownerKey,
                                       TusException exception) throws IOException {

        int status = exception.getStatus();
        String message = exception.getMessage();

        log.warn("Unable to process request {} {}. Sent response status {} with message \"{}\"",
                method, request.getRequestURL(), status, message);

        try {
            for (TusExtension feature : enabledFeatures.values()) {

                if (!request.isProcessedBy(feature)) {
                    request.addProcessor(feature);
                    feature.handleError(method, request, response, uploadStorageService, ownerKey);
                }
            }

            //Since an error occurred, the bytes we have written are probably not valid. So remove them.
            Optional<UploadInfo> uploadInfo = uploadStorageService.getUploadInfo(request.getRequestURI(), ownerKey);
            uploadStorageService.removeLastNumberOfBytes(uploadInfo.get(), request.getBytesRead());

        } catch (TusException ex) {
            log.warn("An exception occurred while handling another exception", ex);
        }

        response.sendError(status, message);
    }

    private void updateSupportedHttpMethods() {
        supportedHttpMethods.clear();
        for (TusExtension tusFeature : enabledFeatures.values()) {
            supportedHttpMethods.addAll(tusFeature.getMinimalSupportedHttpMethods());
        }
    }

    /**
     *
     *
     * @return Builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private UploadStorageService storage;
        private UploadLockingService locking;
        private UploadIdService id;
        private Long maxUploadSize;
        private Set<Class<? extends TusExtension>> userExtensions = new HashSet<>();
        private Set<Class<? extends TusExtension>> coreExtensions;
        private String endpoint;
        private Long expirePeriode;
        private boolean chunkedTransferDecoding;

        private Builder() {
            // Set Core extension lazily
            coreExtensions = Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    CoreProtocol.class,
                                    CreationExtension.class,
                                    ChecksumExtension.class,
                                    TerminationExtension.class,
                                    ExpirationExtension.class,
                                    ConcatenationExtension.class
                            )));
        }

        private static TusExtension createTusExtension(Class<? extends TusExtension> clazz) {
            try {
                return clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Could not create instance for extension", e);
            }
        }


        public TusFileUploadService build() {
            TusFileUploadService service = new TusFileUploadService();

            // add user extension
            Stream.of(userExtensions, coreExtensions).flatMap(Collection::stream)
                    .distinct()
                    .map(Builder::createTusExtension)
                    .forEach(service::addTusExtension);

            service.uploadStorageService = this.storage;
            service.uploadLockingService = this.locking;
            service.uploadIdService = this.id;
            service.endpoint = this.endpoint;
            service.isChunkedTransferDecodingEnabled = this.chunkedTransferDecoding;
            service.uploadStorageService.setMaxUploadSize(this.maxUploadSize);
            service.uploadStorageService.setUploadExpirationPeriod(this.expirePeriode);
            return service;
        }

        /**
         * Provide a custom {@link UploadStorageService} implementation that should be used to store uploaded bytes and
         * metadata ({@link UploadInfo}).
         *
         * @param service The custom {@link UploadStorageService} implementation
         * @return The builder
         */
        public Builder withStorageService(UploadStorageService service) {
            this.storage = service;
            return this;
        }

        /**
         * Provide a custom {@link UploadLockingService} implementation that should be used when processing uploads.
         * The upload locking service is responsible for locking an upload that is being processed so that it cannot
         * be corrupted by simultaneous or delayed requests.
         *
         * @param service The {@link UploadLockingService} implementation to use
         * @return The builder
         */
        public Builder withLockingService(UploadLockingService service) {
            this.locking = service;
            return this;
        }

        /**
         * Provide a custom {@link UploadIdService} implementation that should be used to generate identifiers for
         * the different uploads. Example implementation are {@link me.desair.tus.server.upload.UUID4UploadIdService} and
         * {@link me.desair.tus.server.upload.TimeBasedUploadIdFactory}.
         *
         * @param service The custom {@link UploadIdService} implementation
         * @return The builder
         */
        public Builder withIdService(UploadIdService service) {
            this.id = service;
            return this;
        }

        /**
         * The endpoint's path for the {@link TusFileUploadService}.
         *
         * For example for the URL https://host.tld/app/upload: /app is the deployment's name and /upload is the endpoint.
         * @param endpointUri the endpoint within a deployment where TUS requests will be processed
         * @return The builder
         */
        public Builder withServiceEndpointUri(String endpointUri) {
            this.endpoint = endpointUri;
            return this;
        }

        /**
         * Initializes {@link TusFileUploadService} with default local disk services {@link DiskStorageService} and
         * {@link DiskLockingService}, as well as {@link UUIDUploadIdFactory} for Id creation.
         * The uploaded files will be stored in a subfolder "tus" in the systems temporary directory.
         */
        public Builder withDefaultServices() {
            Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
            Path storagePath = tempPath.resolve("tus");
            log.warn("Using temporary folder for storage/locking. Path: {}", storagePath);

            UUIDUploadIdFactory idFactory = new UUIDUploadIdFactory();
            this.storage = new DiskStorageService(idFactory, storagePath.toString());
            this.locking = new DiskLockingService(idFactory, storagePath.toString());
            return this;
        }

        public Builder withTusExtension(Class<? extends TusExtension> extension) {
            userExtensions.add(extension);
            return this;
        }

        /**
         * Specify the maximum number of bytes that can be uploaded per upload.
         * If you don't call this method, the maximum number of bytes is Long.MAX_VALUE.
         *
         * @param maxUploadSize The maximum upload length that is allowed
         * @return The builer
         */
        public Builder withMaxUploadSize(Long maxUploadSize) {
            if (maxUploadSize <= 0) {
                throw new IllegalArgumentException("The max upload size must be bigger than 0");
            }
            this.maxUploadSize = maxUploadSize;
            return this;
        }

        /**
         * You can set the number of milliseconds after which an upload is considered as expired and available for cleanup.
         *
         * @param expirationPeriod The number of milliseconds after which an upload expires and can be removed
         * @return The builder
         */
        public Builder withUploadExpirationPeriod(long expirationPeriod) {
            this.expirePeriode = expirationPeriod;
            return this;
        }

        /**
         * Instruct this service to (not) decode any requests with Transfer-Encoding value "chunked".
         * Use this method in case the web container in which this service is running does not decode
         * chunked transfers itself. By default, chunked decoding is disabled.
         *
         * @param doChunkedTransferDecoding True if chunked requests should be decoded, false otherwise.
         * @return The builder
         */
        public Builder withChunkedTransferDecoding(boolean doChunkedTransferDecoding) {
            this.chunkedTransferDecoding = doChunkedTransferDecoding;
            return this;

        }
    }

}
