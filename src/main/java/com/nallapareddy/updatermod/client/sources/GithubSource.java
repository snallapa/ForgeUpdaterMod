package com.nallapareddy.updatermod.client.sources;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GithubSource extends BaseSource {

    private static final Logger LOGGER = LogManager.getLogger();

    private final URL releaseUrl;
    private static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 30);

    public GithubSource(String latestVersion, String repo, String owner) throws IllegalArgumentException {
        super(latestVersion);
        try {
            releaseUrl = new URL(String.format("https://api.github.com/repos/%s/%s/releases/tags/v%s", owner, repo, latestVersion));
            LOGGER.info("Github Source URL {}", releaseUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Github Source could not be created! Invalid repo owner version provided");
        }
    }

    @Override
    public void download(Path filePath) throws IOException {
        URL currentUrl = releaseUrl;
        for (int redirects = 0; redirects < MAX_HTTP_REDIRECTS; redirects++)
        {
            URLConnection c = currentUrl.openConnection();
            if (c instanceof HttpURLConnection)
            {
                HttpURLConnection huc = (HttpURLConnection) c;
                huc.setRequestProperty("accept", "application/vnd.github.v3+json");
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
                    LOGGER.error("Error fetching github!!! {}",
                            new String(ByteStreams.toByteArray(huc.getErrorStream()), StandardCharsets.UTF_8));
                }
            }

            InputStream con = c.getInputStream();
            String data = new String(ByteStreams.toByteArray(con), StandardCharsets.UTF_8);
            con.close();

            @SuppressWarnings("unchecked")
            Map<String, Object> json = new Gson().fromJson(data, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> assets = (List<Map<String, String>>)json.get("assets");
            Map<String, String> latest = assets.get(0);
            String browserDownloadURL = latest.get("browser_download_url");
            String assetName = latest.get("name");
            LOGGER.info("Downloading release {} to {}", browserDownloadURL, Paths.get(filePath.toAbsolutePath().toString(), assetName));
            downloadFile(new URL(browserDownloadURL), Paths.get(filePath.toAbsolutePath().toString(), assetName));
            return;
        }
        throw new IOException("Too many redirects while trying to fetch " + releaseUrl);
    }
}
