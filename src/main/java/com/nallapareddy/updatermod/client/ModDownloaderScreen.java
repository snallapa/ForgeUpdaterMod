package com.nallapareddy.updatermod.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nallapareddy.updatermod.client.sources.Source;
import com.nallapareddy.updatermod.client.sources.SupportedSources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.loading.FMLPaths;
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
import java.util.Map;
import java.util.Set;

public class ModDownloaderScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 20);
    private TextFieldWidget downloadUrl;
    private final Screen parentScreen;
    private static final int PADDING = 6;
    private volatile DownloadStatus downloadStatus = DownloadStatus.NONE;

    public ModDownloaderScreen(Screen parentScreen) {
        super(new TranslationTextComponent("updater.download.screen"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        int downloadWidth = this.width < 150 ? this.width - PADDING * 2 : (this.width/2);
        int x = this.width / 4 + PADDING;
        downloadUrl = new TextFieldWidget(font,
                x,
                height / 2 - 30 - PADDING,
                downloadWidth,
                25,
                new TranslationTextComponent("updater.download.url"));
        downloadUrl.setMaxLength(550);
        downloadUrl.setFocus(true);
        downloadUrl.setCanLoseFocus(true);
        children.add(downloadUrl);
        int buttonWidth = downloadWidth / 2 - PADDING;
        this.addButton(new Button(x + 4, height / 2 + PADDING, buttonWidth, 20,
                new TranslationTextComponent("gui.done"), b -> ModDownloaderScreen.this.onClose()));
        this.addButton(new Button(x + (downloadWidth - 2 * buttonWidth - PADDING) + buttonWidth + PADDING, height / 2 + PADDING, buttonWidth, 20,
                new TranslationTextComponent("updater.download"), b -> ModDownloaderScreen.this.onDownload()));
    }

    @Override
    public void render(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(mStack);
        ITextComponent text = new TranslationTextComponent("updater.download.url");
        int x = downloadUrl.x + PADDING/2;
        font.draw(mStack, text.getVisualOrderText(), x, downloadUrl.y - font.lineHeight - 3, 0xFFFFFF);
        if (downloadStatus != DownloadStatus.NONE) {

            ITextComponent messageText = new TranslationTextComponent(downloadStatus.text);
            int messageWidth = font.width(messageText);
            x = (downloadUrl.getWidth() - messageWidth) / 2 + downloadUrl.x;
            int y = downloadUrl.y +downloadUrl.getHeight() + PADDING + 20 + PADDING * 3;
            font.draw(mStack, messageText.getVisualOrderText(), x, y, downloadStatus.color);
        }
        this.downloadUrl.render(mStack, mouseX, mouseY, partialTicks);
        super.render(mStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void resize(Minecraft p_231152_1_, int p_231152_2_, int p_231152_3_) {
        String s = this.downloadUrl.getValue();
        this.init(p_231152_1_, p_231152_2_, p_231152_3_);
        this.downloadUrl.setValue(s);
    }

    @Override
    public void tick() {
        downloadUrl.tick();
    }

    @Override
    public void onClose()
    {
        this.minecraft.setScreen(this.parentScreen);
    }

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

    public void onDownload() {
        Path modDirectory = FMLPaths.MODSDIR.get();
        LOGGER.info("Mods Directory {}" , modDirectory);
        String urlString = downloadUrl.getValue();
        try {
            URL url = new URL(urlString);
            downloadStatus = DownloadStatus.LOADING;
            new Thread(() -> {
                InputStream con = null;
                try {
                    con = openUrlStream(url);
                    String data = new String(ByteStreams.toByteArray(con), StandardCharsets.UTF_8);
                    con.close();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> values = new Gson().fromJson(data, Map.class);
                    LOGGER.info("received JSON {}", data);
                    Set<Map.Entry<String, Object>> mods = values.entrySet();
                    for (Map.Entry<String, Object> mod : mods) {
                        String modName = mod.getKey();
                        Map<String, String> jsonMod = (Map<String, String>) mod.getValue();
                        String source = jsonMod.get("source");
                        SupportedSources supportedSources = SupportedSources.parseSource(source);
                        if (supportedSources == null) {
                            LOGGER.error("not a supported source! {} for {}", source, modName);
                        }
                        Source updateSource = supportedSources.getParser().parse(jsonMod);
                        try {
                            updateSource.download(modDirectory);
                        } catch (IOException e) {
                            LOGGER.error("error downloading! {} for {}", source, modName);
                            throw e;
                        }
                    }
                    downloadStatus = DownloadStatus.SUCCESS;
                } catch (IOException e) {
                    LOGGER.error("Could not download mod {}", urlString, e);
                    downloadStatus = DownloadStatus.ERROR;
                } catch (Exception e) {
                    LOGGER.error("error!! ", e);
                    downloadStatus = DownloadStatus.ERROR;
                }
            }).start();
        } catch (MalformedURLException e) {
            LOGGER.error("invalid URL! {}", urlString, e);
            downloadStatus = DownloadStatus.ERROR;
        }
    }

    enum DownloadStatus {
        NONE("", 0x000000),
        LOADING("updater.download.loading", 0xFFFF00),
        SUCCESS("updater.download.success", 0x00FF00),
        ERROR("updater.download.error", 0xFF0000);

        String text;
        int color;

        DownloadStatus(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}
