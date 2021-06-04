package com.nallapareddy.updatermod.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.MavenVersionStringHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import net.minecraftforge.fml.client.gui.widget.ModListWidget;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ModListScreenWithUpdater extends ModListScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PADDING = 6;

    private int listWidth;
    private List<ModInfo> mods;
    private ModListWidget.ModEntry selected = null;
    private Button updateButton;
    private Button downloadButton;

    public ModListScreenWithUpdater(Screen parentScreen) {
        super(parentScreen);
        this.mods = Collections.unmodifiableList(ModList.get().getMods());
    }

    @Override
    public void init() {
        super.init();
        for (ModInfo mod : mods)
        {
            listWidth = Math.max(listWidth,getFontRenderer().width(mod.getDisplayName()) + 10);
            listWidth = Math.max(listWidth,getFontRenderer().width(MavenVersionStringHelper.artifactVersionToString(mod.getVersion())) + 5);
        }
        listWidth = Math.max(Math.min(listWidth, width/3), 100);
        listWidth += listWidth % 3 != 0 ? (3 - listWidth % 3) : 0;
        int modInfoWidth = this.width - this.listWidth - (PADDING * 3);

        int updaterButtonWidth = Math.max(this.width/6, 30);

        int buttonY = this.height - 20 - PADDING;
        int updateX = this.width - PADDING - updaterButtonWidth;

        this.buttons.stream().filter(w -> w.getMessage().equals(new TranslationTextComponent("gui.done"))).findFirst().ifPresent(doneButton -> {
            doneButton.x = this.listWidth + PADDING * 2;
            doneButton.setWidth(modInfoWidth - updaterButtonWidth * 2 - PADDING * 3);
        });
        this.addButton(this.updateButton = new Button(updateX, buttonY, updaterButtonWidth, 20,
                new TranslationTextComponent("updater.updater"), b -> ModListScreenWithUpdater.this.onUpdate()));
        this.updateButton.active = false;
        int downloadX = updateX - PADDING - updaterButtonWidth;
        this.addButton(this.downloadButton = new Button(downloadX, buttonY, updaterButtonWidth, 20,
                new TranslationTextComponent("updater.download"), b -> ModListScreenWithUpdater.this.onDownload()));
        this.downloadButton.active = true;
    }

    @Override
    public void setSelected(ModListWidget.ModEntry entry) {
        super.setSelected(entry);
        this.selected = entry == this.selected ? null : entry;
        checkUpdate();
    }

    public void checkUpdate() {
        ModInfo selectedMod = this.selected.getInfo();
        VersionChecker.CheckResult vercheck = VersionChecker.getResult(selectedMod);
        UpdateChecker.CheckResult upcheck = UpdateChecker.getResult(selectedMod);
        this.updateButton.active = canUpdate(vercheck, upcheck);
    }

    public static boolean canUpdate(VersionChecker.CheckResult vercheck, UpdateChecker.CheckResult upcheck) {
        return (vercheck.status == VersionChecker.Status.OUTDATED
                || vercheck.status == VersionChecker.Status.BETA_OUTDATED)
                && upcheck.status == UpdateChecker.Status.COMPATIBLE;
    }

    public void onUpdate() {
        ModInfo info = this.selected.getInfo();
        UpdateChecker.CheckResult upcheck = UpdateChecker.getResult(info);
        boolean delete = info.getOwningFile().getFile().getFilePath().toFile().delete();
        if (!delete) {
            LOGGER.error("Could not delete mod {}", info.getModId());
        }
        try {
            upcheck.source.download(FMLPaths.MODSDIR.get());
        } catch (IOException e) {
            LOGGER.error("Could not update mod! {}", info.getModId());
        }
    }

    public void onDownload() {
        this.minecraft.setScreen(new ModDownloaderScreen(this));
    }
}
