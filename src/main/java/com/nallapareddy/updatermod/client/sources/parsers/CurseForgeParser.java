package com.nallapareddy.updatermod.client.sources.parsers;

import com.nallapareddy.updatermod.client.sources.CurseForgeSource;

import java.util.Map;

public class CurseForgeParser extends BaseParser<CurseForgeSource> {
    @Override
    public CurseForgeSource parse(Map<String, String> json) throws Exception {
        if (!json.containsKey("project_id")) {
            throw new Exception("Failed to parse missing project id");
        }
        if (!json.containsKey("version")) {
            throw new Exception("Failed to parse version");
        }

        String repo = json.get("project_id");
        String owner = json.getOrDefault("release_type", "release");
        String latest = json.get("version");
        return new CurseForgeSource(latest, repo, owner);
    }
}
