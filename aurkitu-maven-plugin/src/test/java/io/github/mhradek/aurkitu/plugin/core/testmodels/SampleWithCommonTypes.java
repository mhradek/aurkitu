package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Date;
import java.util.Set;
import java.util.Collection;

@FlatBufferTable
public class SampleWithCommonTypes {
    private Date created;
    private URL link;
    private BigDecimal amount;
    private Boolean flag;
    private Integer count;
    private Set<String> tags;
    private Collection<Integer> numbers;
    private byte[] bytes;
}
