package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Instrumentator {

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    final String[] args = tokenize(argsString);
    final ProjectData data = ProjectData.createProjectData(new File(args[0]), Boolean.valueOf(args[1]).booleanValue(), Boolean.valueOf(args[2]).booleanValue());
    final boolean isSample = Boolean.valueOf(args[3]).booleanValue();
    final List includePatterns = new ArrayList();
    final Perl5Compiler pc = new Perl5Compiler();
    final String excludes = "-exclude";
    int i = 4;
    for (; i < args.length; i++) {
      if (excludes.equals(args[i])) break;
      includePatterns.add(pc.compile(args[i]));
    }
    data.setIncludePatterns(includePatterns);
    final List excludePatterns = new ArrayList();
    for (; i < args.length; i++) {
      excludePatterns.add(pc.compile(args[i]));
    }
    data.setExcludePatterns(excludePatterns);
    final Perl5Matcher pm = new Perl5Matcher();
    instrumentation.addTransformer(new ClassFileTransformer() {
      public byte[] transform(ClassLoader loader,
                              String className,
                              Class classBeingRedefined,
                              ProtectionDomain protectionDomain,
                              byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
          if (className == null) {
            return null;
          }
          if (className.endsWith(".class")) {
            className = className.substring(0, className.length() - 6);
          }
          className = className.replace('\\', '.').replace('/', '.');
          for (Iterator it = excludePatterns.iterator(); it.hasNext();) {
            if (pm.matches(className, (Pattern)it.next())) return null;
          }
          if (includePatterns.isEmpty() && loader != null) {
            return instrument(classfileBuffer, isSample, data);
          }
          for (Iterator it = includePatterns.iterator(); it.hasNext();) {
            if (pm.matches(className, (Pattern)it.next())) {
              return instrument(classfileBuffer, isSample, data);
            }
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    });
  }

  private static byte[] instrument(final byte[] classfileBuffer, final boolean sample, final ProjectData data) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    final ClassAdapter cv = sample ? (ClassAdapter)new SamplingInstrumenter(data, cw) : new ClassInstrumenter(data, cw);
    cr.accept(cv, 0);
    return cw.toByteArray();
  }

  private static String[] tokenize(String argumentString) {
    List tokenizedArgs = new ArrayList();
    StringBuffer currentArg = new StringBuffer();
    for (int i = 0; i < argumentString.length(); i++) {
      char c = argumentString.charAt(i);
      switch (c) {
        default:
          currentArg.append(c);
          break;
        case ' ':
          String arg = currentArg.toString();
          if (arg.length() > 0) {
            tokenizedArgs.add(arg);
          }
          currentArg = new StringBuffer();
          break;
        case '\"':
          for (i++; i < argumentString.length(); i++) {
            char d = argumentString.charAt(i);
            if (d == '\"') {
              break;
            }
            currentArg.append(d);
          }
      }
    }

    String arg = currentArg.toString();
    if (arg.length() > 0) {
      tokenizedArgs.add(arg);
    }
    return (String[])tokenizedArgs.toArray(new String[tokenizedArgs.size()]);
  }
}