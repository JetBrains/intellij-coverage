package com.intellij.rt.coverage.util;

import gnu.trove.TIntObjectHashMap;

/**
 * @author Pavel.Sher
 */
public class StringsPool {
  private final static TIntObjectHashMap myReusableStrings = new TIntObjectHashMap(30000);

  public static String getFromPool(String value) {
    if (value == null) return null;

    final int hash = value.hashCode();
    String reused = (String) myReusableStrings.get(hash);
    if (reused != null) return reused;
    // new String() is required because value often is passed as substring which has a reference to original char array
    // see {@link String.substring(int, int} method implementation.
    //noinspection RedundantStringConstructorCall
    reused = new String(value);
    myReusableStrings.put(hash, reused);
    return reused;
  }
}
