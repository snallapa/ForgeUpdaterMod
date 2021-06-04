package com.nallapareddy.updatermod.client.sources.parsers;

import com.nallapareddy.updatermod.client.sources.GithubSource;

import java.util.Map;

public class GithubParser extends BaseParser<GithubSource> {
    @Override
    public GithubSource parse(Map<String, String> json) throws Exception {
        if (!json.containsKey("repo")) {
            throw new Exception("Failed to parse missing repo");
        }
        if (!json.containsKey("owner")) {
            throw new Exception("Failed to parse missing owner");
        }
        if (!json.containsKey("version")) {
            throw new Exception("UPDATER ERROR");
        }
        String repo = json.get("repo");
        String owner = json.get("owner");
        String latest = json.get("version");
        return new GithubSource(latest, repo, owner);
    }
}
