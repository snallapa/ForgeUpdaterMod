package com.nallapareddy.updatermod.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.nallapareddy.updatermod.client.sources.Source;
import com.nallapareddy.updatermod.client.sources.SupportedSources;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.versions.mcp.MCPVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateChecker
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 20);

    public enum Status
    {
        COMPATIBLE,
        INCOMPATIBLE,
        PENDING
    }

    public static class CheckResult
    {
        @Nonnull
        public final Status status;
        @Nullable
        public final Source source;

        private CheckResult(@Nonnull Status status, @Nullable Source source)
        {
            this.status = status;
            this.source = source;
        }

        @Override
        public String toString() {
            return String.format("status: %s and source: %s", status, source);
        }
    }

    public static void startUpdaterCheck()
    {
        new Thread("Update Checker")
        {
            @Override
            public void run()
            {
                gatherMods().forEach(this::process);
            }

            /**
             * Opens stream for given URL while following redirects
             */
            private InputStream openUrlStream(URL url) throws IOException
            {
                URL currentUrl = url;
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
                    }

                    return c.getInputStream();
                }
                throw new IOException("Too many redirects while trying to fetch " + url);
            }

            @SuppressWarnings("UnstableApiUsage")
            private void process(IModInfo mod)
            {
                Status status;
                try
                {
                    URL url = mod.getUpdateURL();
                    LOGGER.info("[{}] Checking updates for {}", mod.getModId(), url.toString());

                    InputStream con = openUrlStream(url);
                    String data = new String(ByteStreams.toByteArray(con), StandardCharsets.UTF_8);
                    con.close();

                    LOGGER.debug("[{}] Received update check data:\n{}", mod.getModId(), data);


                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = new Gson().fromJson(data, Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, String> promos = (Map<String, String>)json.get("promos");
                    String mcVersion = MCPVersion.getMCVersion();
                    String latest = promos.get(mcVersion + "-latest");

                    if (json.containsKey("updater")) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> updater = (Map<String, String>)json.get("updater");
                        updater.put("version", latest);
                        String source = updater.get("source");
                        SupportedSources supportedSources = SupportedSources.parseSource(source);
                        if (supportedSources == null) {
                            throw new RuntimeException("Unknown source");
                        }
                        Source updateSource = supportedSources.getParser().parse(updater);
                        results.put(mod, new UpdateChecker.CheckResult(Status.COMPATIBLE, updateSource));
                    } else {
                        results.put(mod, new UpdateChecker.CheckResult(Status.INCOMPATIBLE, null));
                    }
                }
                catch (Exception e)
                {
                    LOGGER.warn("Failed to process update information", e);
                    results.put(mod, new UpdateChecker.CheckResult(Status.INCOMPATIBLE, null));
                }
            }
        }.start();
    }

    // Gather a list of mods that have opted in to this update system by providing a URL.
    private static List<IModInfo> gatherMods()
    {
        List<IModInfo> ret = new LinkedList<>();
        for (ModInfo info : ModList.get().getMods()) {
            URL url = info.getUpdateURL();
            if (url != null)
                ret.add(info);
        }
        return ret;
    }

    private static Map<IModInfo, UpdateChecker.CheckResult > results = new ConcurrentHashMap<>();
    private static final UpdateChecker.CheckResult PENDING_CHECK = new UpdateChecker.CheckResult(Status.PENDING, null);

    public static UpdateChecker.CheckResult getResult(IModInfo mod)
    {
        return results.getOrDefault(mod, PENDING_CHECK);
    }

}