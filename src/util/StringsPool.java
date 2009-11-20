package com.intellij.rt.coverage.util;

import gnu.trove.TLongObjectHashMap;

/**
 * @author Pavel.Sher
 */
public class StringsPool {
  private final static TLongObjectHashMap myReusableStrings = new TLongObjectHashMap(30000);

  public static String getFromPool(String value) {
    if (value == null) return null;

    final long hash = StringHash.calc(value);
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
