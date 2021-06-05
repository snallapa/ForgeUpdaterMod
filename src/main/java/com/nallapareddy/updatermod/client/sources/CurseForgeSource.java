package com.nallapareddy.updatermod.client.sources;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CurseForgeSource extends BaseSource {

    private String projectId;
    private String releaseType;

    public CurseForgeSource(String latestVersion, String projectId, String releaseType) {
        super(latestVersion);
        this.projectId = projectId;
        this.releaseType = releaseType;
    }

    @Override
    public void download(Path filePath) throws IOException {
        URL currentUrl = new URL(String.format("https://api.cfwidget.com/minecraft/mc-mods/%s", this.projectId));
        for (int redirects = 0; redirects < MAX_HTTP_REDIRECTS; redirects++)
        {
            URLConnection c = currentUrl.openConnection();
            if (c instanceof HttpURLConnection)
            {
                HttpURLConnection huc = (HttpURLConnection) c;
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
                    LOGGER.error("Error fetching curse forge source!!! {}",
                            new String(ByteStreams.toByteArray(huc.getErrorStream()), StandardCharsets.UTF_8));
                }
            }

            InputStream con = c.getInputStream();
            String data = new String(ByteStreams.toByteArray(con), StandardCharsets.UTF_8);
            con.close();

            @SuppressWarnings("unchecked")
            Map<String, Object> json = new Gson().fromJson(data, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>)json.get("files");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> file = files.stream()
                    .filter(f -> {
                        @SuppressWarnings("unchecked")
                        ArrayList<String> versions = (ArrayList<String>) f.get("versions");
                        return f.get("type").equals(this.releaseType) && versions.contains(latestVersion);
                    })
                    .collect(Collectors.toList());
            if (file.size() < 1) {
                LOGGER.error("not found file in {} ", files);
                throw new IOException("Cannot update Curse Mod: " + projectId);
            }
            if (file.size() > 1) {
                LOGGER.info("more than one release found... just picking the first one");
            }
            Map<String, Object> fileInfo = file.get(0);
            String id = ((int)((double) fileInfo.get("id")))  + "";
            String assetName = (String) fileInfo.get("name");
            String browserDownloadURL = "https://media.forgecdn.net/files/" + id.substring(0, 4) + "/" + id.substring(4) + "/" + assetName;
            Path downloadPath = Paths.get(filePath.toAbsolutePath().toString(), assetName);
            LOGGER.info("Downloading mod {} to {}", browserDownloadURL, downloadPath);
            downloadFile(new URL(browserDownloadURL), downloadPath);
            return;
        }
        throw new IOException("Too many redirects while trying to fetch " + currentUrl);
    }
}
