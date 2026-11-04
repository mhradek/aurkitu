package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Content-aware schema cache. Hash covers config and scanned type names.
 */
@Slf4j
final class SchemaCache {

    private SchemaCache() {}

    static String computeHash(SchemaGenerator.Config config, String namespace, ScanResult scanResult) {
        String configHash = Integer.toHexString(Objects.hash(
                config.specifiedPackages, config.validate, config.ignoreStaticMembers,
                config.fileIdentifier, config.includes, config.mapCommonTypes, namespace,
                config.snakeCaseFields, config.generateVersion));
        StringBuilder typeNames = new StringBuilder();
        for (ClassInfo ci : scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName())) {
            typeNames.append(ci.getName()).append(';');
        }
        for (ClassInfo ci : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
            typeNames.append(ci.getName()).append(';');
        }
        return Integer.toHexString(Objects.hash(configHash, typeNames.toString()));
    }

    static boolean isHit(boolean cacheEnabled, File outFile, File cacheFile, String inputHash) {
        if (!cacheEnabled || !outFile.exists() || !cacheFile.exists()) {
            return false;
        }
        try {
            String cached = Files.readString(cacheFile.toPath()).trim();
            return inputHash.equals(cached);
        } catch (IOException ignored) {
            return false;
        }
    }

    static void write(File cacheFile, String inputHash) throws IOException {
        Files.writeString(cacheFile.toPath(), inputHash);
    }
}
