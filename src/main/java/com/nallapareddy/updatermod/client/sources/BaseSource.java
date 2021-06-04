package com.nallapareddy.updatermod.client.sources;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public abstract class BaseSource implements Source {
    protected String latestVersion;

    public BaseSource(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public void downloadFile(URL url, Path downloadPath) throws IOException {
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(downloadPath.toAbsolutePath().toString());
        fileOutputStream.getChannel()
                .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

    public abstract void download(Path filePath) throws IOException;

}
