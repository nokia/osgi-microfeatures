package com.alcatel.as.http2.client.api.impl;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;
import com.alcatel.as.http2.client.api.HttpResponse.BodyHandler;
import com.alcatel.as.http2.client.api.HttpResponse.ResponseInfo;
import com.alcatel.as.http2.client.api.HttpResponse.BodySubscriber;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alcatel.as.http2.client.api.impl.ResponseSubscribers.PathSubscriber;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class ResponseBodyHandlers {

    private ResponseBodyHandlers() { }

    private static final String pathForSecurityCheck(Path path) {
        return path.toFile().getPath();
    }

    /**
     * A Path body handler.
     */
    public static class PathBodyHandler implements BodyHandler<Path>{
        private final Path file;
        private final List<OpenOption> openOptions;  // immutable list
        private final FilePermission filePermission;

        /**
         * Factory for creating PathBodyHandler.
         *
         * Permission checks are performed here before construction of the
         * PathBodyHandler. Permission checking and construction are
         * deliberately and tightly co-located.
         */
        public static PathBodyHandler create(Path file,
                                             List<OpenOption> openOptions) {
            FilePermission filePermission = null;
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                String fn = pathForSecurityCheck(file);
                FilePermission writePermission = new FilePermission(fn, "write");
                sm.checkPermission(writePermission);
                filePermission = writePermission;
            }
            return new PathBodyHandler(file, openOptions, filePermission);
        }

        private PathBodyHandler(Path file,
                                List<OpenOption> openOptions,
                                FilePermission filePermission) {
            this.file = file;
            this.openOptions = openOptions;
            this.filePermission = filePermission;
        }

        @Override
        public BodySubscriber<Path> apply(ResponseInfo responseInfo) {
            return new PathSubscriber(file, openOptions, filePermission);
        }
    }

    /** With push promise Map implementation */
    public static class PushPromisesHandlerWithMap<T>
        implements HttpResponse.PushPromiseHandler<T>
    {
        private final ConcurrentMap<HttpRequest,CompletableFuture<HttpResponse<T>>> pushPromisesMap;
        private final Function<HttpRequest,BodyHandler<T>> pushPromiseHandler;

        public PushPromisesHandlerWithMap(Function<HttpRequest,BodyHandler<T>> pushPromiseHandler,
                                          ConcurrentMap<HttpRequest,CompletableFuture<HttpResponse<T>>> pushPromisesMap) {
            this.pushPromiseHandler = pushPromiseHandler;
            this.pushPromisesMap = pushPromisesMap;
        }

        @Override
        public void applyPushPromise(
                HttpRequest initiatingRequest, HttpRequest pushRequest,
                Function<BodyHandler<T>,CompletableFuture<HttpResponse<T>>> acceptor)
        {
            URI initiatingURI = initiatingRequest.uri();
            URI pushRequestURI = pushRequest.uri();
            if (!initiatingURI.getHost().equalsIgnoreCase(pushRequestURI.getHost()))
                return;

            int initiatingPort = initiatingURI.getPort();
            if (initiatingPort == -1 ) {
                if ("https".equalsIgnoreCase(initiatingURI.getScheme()))
                    initiatingPort = 443;
                else
                    initiatingPort = 80;
            }
            int pushPort = pushRequestURI.getPort();
            if (pushPort == -1 ) {
                if ("https".equalsIgnoreCase(pushRequestURI.getScheme()))
                    pushPort = 443;
                else
                    pushPort = 80;
            }
            if (initiatingPort != pushPort)
                return;

            CompletableFuture<HttpResponse<T>> cf =
                    acceptor.apply(pushPromiseHandler.apply(pushRequest));
            pushPromisesMap.put(pushRequest, cf);
        }
    }

    // Similar to Path body handler, but for file download.
    public static class FileDownloadBodyHandler implements BodyHandler<Path> {
        private final Path directory;
        private final List<OpenOption> openOptions;
        private final FilePermission[] filePermissions;  // may be null

        /**
         * Factory for creating FileDownloadBodyHandler.
         *
         * Permission checks are performed here before construction of the
         * FileDownloadBodyHandler. Permission checking and construction are
         * deliberately and tightly co-located.
         */
        public static FileDownloadBodyHandler create(Path directory,
                                                     List<OpenOption> openOptions) {
            FilePermission filePermissions[] = null;
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                String fn = pathForSecurityCheck(directory);
                FilePermission writePermission = new FilePermission(fn, "write");
                String writePathPerm = fn + File.separatorChar + "*";
                FilePermission writeInDirPermission = new FilePermission(writePathPerm, "write");
                sm.checkPermission(writeInDirPermission);
                FilePermission readPermission = new FilePermission(fn, "read");
                sm.checkPermission(readPermission);

                // read permission is only needed before determine the below checks
                // only write permission is required when downloading to the file
                filePermissions = new FilePermission[] { writePermission, writeInDirPermission };
            }

            // existence, etc, checks must be after permission checks
            if (Files.notExists(directory))
                throw new IllegalArgumentException("non-existent directory: " + directory);
            if (!Files.isDirectory(directory))
                throw new IllegalArgumentException("not a directory: " + directory);
            if (!Files.isWritable(directory))
                throw new IllegalArgumentException("non-writable directory: " + directory);

            return new FileDownloadBodyHandler(directory, openOptions, filePermissions);

        }

        private FileDownloadBodyHandler(Path directory,
                                       List<OpenOption> openOptions,
                                       FilePermission... filePermissions) {
            this.directory = directory;
            this.openOptions = openOptions;
            this.filePermissions = filePermissions;
        }

        /** The "attachment" disposition-type and separator. */
        static final String DISPOSITION_TYPE = "attachment;";

        /** The "filename" parameter. */
        static final Pattern FILENAME = Pattern.compile("filename\\s*=", CASE_INSENSITIVE);

        static final List<String> PROHIBITED = java.util.Collections.unmodifiableList(Arrays.asList(new String[] {".", "..", "", "~" , "|"}));

        static final UncheckedIOException unchecked(ResponseInfo rinfo,
                                                    String msg) {
            String s = String.format("%s in response [%d, %s]", msg, rinfo.statusCode(), rinfo.headers());
            return new UncheckedIOException(new IOException(s));
        }

        @Override
        public BodySubscriber<Path> apply(ResponseInfo responseInfo) {
            String dispoHeader = responseInfo.headers().firstValue("Content-Disposition")
                    .orElseThrow(() -> unchecked(responseInfo, "No Content-Disposition header"));

            if (!dispoHeader.regionMatches(true, // ignoreCase
                                           0, DISPOSITION_TYPE,
                                           0, DISPOSITION_TYPE.length())) {
                throw unchecked(responseInfo, "Unknown Content-Disposition type");
            }

            Matcher matcher = FILENAME.matcher(dispoHeader);
            if (!matcher.find()) {
                throw unchecked(responseInfo, "Bad Content-Disposition filename parameter");
            }
            int n = matcher.end();

            int semi = dispoHeader.substring(n).indexOf(";");
            String filenameParam;
            if (semi < 0) {
                filenameParam = dispoHeader.substring(n);
            } else {
                filenameParam = dispoHeader.substring(n, n + semi);
            }

            // strip all but the last path segment
            int x = filenameParam.lastIndexOf("/");
            if (x != -1) {
                filenameParam = filenameParam.substring(x+1);
            }
            x = filenameParam.lastIndexOf("\\");
            if (x != -1) {
                filenameParam = filenameParam.substring(x+1);
            }

            filenameParam = filenameParam.trim();

            if (filenameParam.startsWith("\"")) {  // quoted-string
                if (!filenameParam.endsWith("\"") || filenameParam.length() == 1) {
                    throw unchecked(responseInfo,
                            "Badly quoted Content-Disposition filename parameter");
                }
                filenameParam = filenameParam.substring(1, filenameParam.length() -1 );
            } else {  // token,
                if (filenameParam.contains(" ")) {  // space disallowed
                    throw unchecked(responseInfo,
                            "unquoted space in Content-Disposition filename parameter");
                }
            }

            if (PROHIBITED.contains(filenameParam)) {
                throw unchecked(responseInfo,
                        "Prohibited Content-Disposition filename parameter:"
                                + filenameParam);
            }

            Path file = Paths.get(directory.toString(), filenameParam);

            if (!file.startsWith(directory)) {
                throw unchecked(responseInfo,
                        "Resulting file, " + file.toString() + ", outside of given directory");
            }

            return new PathSubscriber(file, openOptions, filePermissions);
        }
    }
}
