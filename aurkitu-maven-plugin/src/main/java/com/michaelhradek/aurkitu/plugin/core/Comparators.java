package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;

import java.util.Comparator;

public class Comparators {

    // Static class
    private Comparators() {}

    public static Comparator<TypeDeclaration> TYPE_DECLARATION = new Comparator<TypeDeclaration>() {
        @Override
        public int compare(TypeDeclaration o1, TypeDeclaration o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    public static Comparator<EnumDeclaration> ENUM_DECLARATION = new Comparator<EnumDeclaration>() {

        @Override
        public int compare(EnumDeclaration o1, EnumDeclaration o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    public static Comparator<Schema.Constant<?>> CONSTANT_DECLARATION = new Comparator<Schema.Constant<?>>() {

        @Override
        public int compare(Schema.Constant<?> o1, Schema.Constant<?> o2) {
            return o1.name.compareToIgnoreCase(o2.name);
        }
    };

    public static Comparator<String> STRING_LIST = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    };

    public static Comparator<TypeDeclaration.Property> TYPE_DECLARATION_PROPERTY = new Comparator<TypeDeclaration.Property>() {

        @Override
        public int compare(TypeDeclaration.Property o1, TypeDeclaration.Property o2) {
            return o1.name.compareToIgnoreCase(o2.name);
        }
    };
}
