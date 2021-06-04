package com.nallapareddy.updatermod.client.sources.parsers;

import com.nallapareddy.updatermod.client.sources.Source;

import java.util.Map;

public abstract class BaseParser<T extends Source> {
    public abstract T parse(Map<String, String> json) throws Exception;
}
