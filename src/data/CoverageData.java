package com.intellij.rt.coverage.data;

import java.io.DataOutputStream;
import java.io.IOException;

public interface CoverageData {
    void save(DataOutputStream os) throws IOException;
    void merge(CoverageData data);
}
