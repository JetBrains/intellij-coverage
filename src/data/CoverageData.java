package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.DictionaryLookup;

import java.io.DataOutputStream;
import java.io.IOException;

public interface CoverageData {
    void save(DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException;
    void merge(CoverageData data);
}
