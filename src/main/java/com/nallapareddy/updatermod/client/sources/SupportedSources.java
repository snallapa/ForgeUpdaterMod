package com.nallapareddy.updatermod.client.sources;

import com.nallapareddy.updatermod.client.sources.parsers.BaseParser;
import com.nallapareddy.updatermod.client.sources.parsers.CurseForgeParser;
import com.nallapareddy.updatermod.client.sources.parsers.GithubParser;

public enum SupportedSources {
    GITHUB("GITHUB", new GithubParser()),
    CURSE("CURSEFORGE", new CurseForgeParser());

    private String name;
    private BaseParser<?> parser;

    SupportedSources(String name, BaseParser<?> parser) {
        this.name = name;
        this.parser = parser;
    }

    public String getName() {
        return name;
    }

    public BaseParser<?> getParser() {
        return parser;
    }

    public static SupportedSources parseSource(String source) {
        SupportedSources[] sources = SupportedSources.values();
        for (SupportedSources supportedSources : sources) {
            if (supportedSources.name.equals(source)) {
                return supportedSources;
            }
        }
        return null;
    }
}
