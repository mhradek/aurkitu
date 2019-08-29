package com.michaelhradek.aurkitu.reflections.scanners;

import com.michaelhradek.aurkitu.reflections.vfs.Vfs;

/** scans classes and stores fqn as key and full path as value.
 * <p>Deprecated. use {@link TypeElementsScanner} */
@Deprecated
public class TypesScanner extends AbstractScanner {

    @Override
    public Object scan(Vfs.File file, Object classObject) {
        classObject = super.scan(file, classObject);
        String className = getMetadataAdapter().getClassName(classObject);
        getStore().put(className, className);
        return classObject;
    }

    @Override
    public void scan(Object cls) {
        throw new UnsupportedOperationException("should not get here");
    }
}