package com.michaelhradek.aurkitu.plugin.core.processing.interfaces;

import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;

import java.lang.reflect.Field;

public interface PropertyExtractor {

    TypeDeclaration.Property process(TypeDeclaration.Property property, Field field, boolean useFullName);
}
