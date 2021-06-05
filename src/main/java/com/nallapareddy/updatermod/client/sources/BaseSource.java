package com.nallapareddy.updatermod.client.sources;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

import static com.nallapareddy.updatermod.client.sources.util.URLUtil.connect;

public abstract class BaseSource implements Source {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected String latestVersion;

    public BaseSource(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public void downloadFile(URL url, Path downloadPath) throws IOException {
        InputStream stream = connect(url);
        ReadableByteChannel readableByteChannel = Channels.newChannel(stream);
        FileOutputStream fileOutputStream = new FileOutputStream(downloadPath.toAbsolutePath().toString());
        fileOutputStream.getChannel()
                .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

    public abstract void download(Path filePath) throws IOException;

}
