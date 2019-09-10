package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ComparatorsTest {

    @Test
    public void testStringListOrdering() {
        List<String> testStringArray = new ArrayList<>();
        testStringArray.add("beta");
        testStringArray.add("gamma");
        testStringArray.add("alpha");

        Assert.assertEquals("beta", testStringArray.get(0));
        Assert.assertEquals("gamma", testStringArray.get(1));
        Assert.assertEquals("alpha", testStringArray.get(2));

        testStringArray.sort(Comparators.STRING_LIST);
        Assert.assertEquals("alpha", testStringArray.get(0));
        Assert.assertEquals("beta", testStringArray.get(1));
        Assert.assertEquals("gamma", testStringArray.get(2));
    }

    @Test
    public void testTypeDeclarationSort() {
        List<TypeDeclaration> typeDeclarationArrayList = new ArrayList<>();
        TypeDeclaration typeAlpha = new TypeDeclaration();
        typeAlpha.setName("alpha");

        TypeDeclaration typeBeta = new TypeDeclaration();
        typeBeta.setName("beta");

        TypeDeclaration typeGamma = new TypeDeclaration();
        typeGamma.setName("gamma");

        typeDeclarationArrayList.add(typeBeta);
        typeDeclarationArrayList.add(typeGamma);
        typeDeclarationArrayList.add(typeAlpha);

        Assert.assertTrue(typeDeclarationArrayList.get(0).getName().equals("beta"));
        Assert.assertTrue(typeDeclarationArrayList.get(1).getName().equals("gamma"));
        Assert.assertTrue(typeDeclarationArrayList.get(2).getName().equals("alpha"));

        typeDeclarationArrayList.sort(Comparators.TYPE_DECLARATION);
        Assert.assertTrue(typeDeclarationArrayList.get(0).getName().equals("alpha"));
        Assert.assertTrue(typeDeclarationArrayList.get(1).getName().equals("beta"));
        Assert.assertTrue(typeDeclarationArrayList.get(2).getName().equals("gamma"));
    }

    @Test
    public void testEnumDeclarationSort() {
        List<EnumDeclaration> enumDeclarationArrayList = new ArrayList<>();
        EnumDeclaration enumAlpha = new EnumDeclaration();
        enumAlpha.setName("alpha");

        EnumDeclaration enumBeta = new EnumDeclaration();
        enumBeta.setName("beta");

        EnumDeclaration enumGamma = new EnumDeclaration();
        enumGamma.setName("gamma");

        enumDeclarationArrayList.add(enumBeta);
        enumDeclarationArrayList.add(enumGamma);
        enumDeclarationArrayList.add(enumAlpha);

        Assert.assertTrue(enumDeclarationArrayList.get(0).getName().equals("beta"));
        Assert.assertTrue(enumDeclarationArrayList.get(1).getName().equals("gamma"));
        Assert.assertTrue(enumDeclarationArrayList.get(2).getName().equals("alpha"));

        enumDeclarationArrayList.sort(Comparators.ENUM_DECLARATION);
        Assert.assertTrue(enumDeclarationArrayList.get(0).getName().equals("alpha"));
        Assert.assertTrue(enumDeclarationArrayList.get(1).getName().equals("beta"));
        Assert.assertTrue(enumDeclarationArrayList.get(2).getName().equals("gamma"));
    }

    @Test
    public void testConstantSort() {
        List<Schema.Constant<Integer>> constantArrayList = new ArrayList<>();
        Schema.Constant<Integer> constantAlpha = new Schema.Constant<>();
        constantAlpha.name = "alpha";

        Schema.Constant<Integer> constantBeta = new Schema.Constant<>();
        constantBeta.name = "beta";

        Schema.Constant<Integer> constantGamma = new Schema.Constant<>();
        constantGamma.name = "gamma";

        constantArrayList.add(constantBeta);
        constantArrayList.add(constantGamma);
        constantArrayList.add(constantAlpha);

        Assert.assertTrue(constantArrayList.get(0).name.equals("beta"));
        Assert.assertTrue(constantArrayList.get(1).name.equals("gamma"));
        Assert.assertTrue(constantArrayList.get(2).name.equals("alpha"));

        constantArrayList.sort(Comparators.CONSTANT_DECLARATION);
        Assert.assertTrue(constantArrayList.get(0).name.equals("alpha"));
        Assert.assertTrue(constantArrayList.get(1).name.equals("beta"));
        Assert.assertTrue(constantArrayList.get(2).name.equals("gamma"));
    }

    @Test
    public void testTypeDeclarationProperty() {
        List<TypeDeclaration.Property> propertyArrayList = new ArrayList<>();
        TypeDeclaration.Property propertyAlpha = new TypeDeclaration.Property();
        propertyAlpha.name = "alpha";

        TypeDeclaration.Property propertyBeta = new TypeDeclaration.Property();
        propertyBeta.name = "beta";

        TypeDeclaration.Property propertyGamma = new TypeDeclaration.Property();
        propertyGamma.name = "gamma";

        propertyArrayList.add(propertyBeta);
        propertyArrayList.add(propertyGamma);
        propertyArrayList.add(propertyAlpha);

        Assert.assertTrue(propertyArrayList.get(0).name.equals("beta"));
        Assert.assertTrue(propertyArrayList.get(1).name.equals("gamma"));
        Assert.assertTrue(propertyArrayList.get(2).name.equals("alpha"));

        propertyArrayList.sort(Comparators.TYPE_DECLARATION_PROPERTY);
        Assert.assertTrue(propertyArrayList.get(0).name.equals("alpha"));
        Assert.assertTrue(propertyArrayList.get(1).name.equals("beta"));
        Assert.assertTrue(propertyArrayList.get(2).name.equals("gamma"));
    }
}
