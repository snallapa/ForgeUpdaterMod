package com.nallapareddy.updatermod.client.sources.util;

import com.google.common.io.ByteStreams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class URLUtil {
    protected static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 50);
    protected static final Logger LOGGER = LogManager.getLogger();


    public static InputStream connect(URL currentUrl) throws IOException {
        String httpAgent = System.getProperty("http.agent");
        LOGGER.info("resetting http agent {}", httpAgent);
        System.setProperty("http.agent", "");
        for (int redirects = 0; redirects < MAX_HTTP_REDIRECTS; redirects++)
        {
            URLConnection c = currentUrl.openConnection();
            if (c instanceof HttpURLConnection) {
                HttpURLConnection huc = (HttpURLConnection) c;
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                c.setRequestProperty("Accept", "*/*");
                huc.setInstanceFollowRedirects(false);
                int responseCode = huc.getResponseCode();
                if (responseCode >= 300 && responseCode <= 399)
                {
                    try
                    {
                        String loc = huc.getHeaderField("Location");
                        currentUrl = new URL(currentUrl, loc);
                        continue;
                    }
                    finally
                    {
                        huc.disconnect();
                    }
                }

                if (responseCode != 200) {
                    String response = new String(ByteStreams.toByteArray(huc.getErrorStream()), StandardCharsets.UTF_8);
                    LOGGER.error("Error fetching curse forge source!!! {}",
                            response);
                    throw new IOException(String.format("Error code %s with response %s", responseCode, response));
                }
            }
            return c.getInputStream();
        }
        throw new IOException("Reached max number of redirects!");
    }

    public static InputStream connect(String browserUrl) throws IOException {
        return connect(new URL(browserUrl));
    }
}
