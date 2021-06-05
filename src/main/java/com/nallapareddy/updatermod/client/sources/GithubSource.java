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

import static com.nallapareddy.updatermod.client.sources.util.URLUtil.connect;

public class GithubSource extends BaseSource {


    private final String releaseUrl;
    public GithubSource(String latestVersion, String repo, String owner) throws IllegalArgumentException {
        super(latestVersion);
        releaseUrl = String.format("https://api.github.com/repos/%s/%s/releases/tags/v%s", owner, repo, latestVersion);
        LOGGER.info("Github Source URL {}", releaseUrl);
    }

    @Override
    public void download(Path filePath) throws IOException {
        InputStream con = connect(releaseUrl);
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
    }
}
