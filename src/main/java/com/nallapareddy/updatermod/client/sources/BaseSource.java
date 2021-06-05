package com.nallapareddy.updatermod.client.sources;

import com.google.common.io.ByteStreams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseSource implements Source {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected String latestVersion;
    protected static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 30);

    public BaseSource(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public void downloadFile(URL url, Path downloadPath) throws IOException {
        System.setProperty("http.agent", "");
        URLConnection urlConnection = url.openConnection();
        if (urlConnection instanceof HttpURLConnection)
        {
            HttpURLConnection huc = (HttpURLConnection) urlConnection;
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            urlConnection.setRequestProperty("Accept", "*/*");
            if (huc.getResponseCode() != 200) {
                LOGGER.error("Error downloading for source!!! {}",
                        new String(ByteStreams.toByteArray(huc.getErrorStream()), StandardCharsets.UTF_8));
            }
        }
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(downloadPath.toAbsolutePath().toString());
        fileOutputStream.getChannel()
                .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    }

    public abstract void download(Path filePath) throws IOException;

}
