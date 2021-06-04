package com.nallapareddy.updatermod.client.sources;

import java.io.IOException;
import java.nio.file.Path;

public interface Source {
    public void download(Path filePath) throws IOException;
}
