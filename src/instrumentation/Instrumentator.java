package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.apache.oro.text.regex.MalformedPatternException;
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
    final boolean traceLines = Boolean.valueOf(args[1]).booleanValue();
    final boolean sampling = Boolean.valueOf(args[4]).booleanValue();
    final ProjectData data = ProjectData.createProjectData(new File(args[0]), traceLines, Boolean.valueOf(args[2]).booleanValue(), Boolean.valueOf(args[3]).booleanValue(), sampling);
    final List includePatterns = new ArrayList();
    final Perl5Compiler pc = new Perl5Compiler();
    System.out.println("---- IDEA coverage runner ---- ");
    System.out.println(sampling ? "sampling ..." : ("tracing " + (traceLines ? "and tracking per test coverage ..." : "...")));
    final String excludes = "-exclude";
    int i = 5;
    System.out.println("include patterns:");
    for (; i < args.length; i++) {
      if (excludes.equals(args[i])) break;
      includePatterns.add(compileRegex(args[i], pc));
      System.out.println(args[i]);
    }
    System.out.println("exclude patterns:");
    final List excludePatterns = new ArrayList();
    for (; i < args.length; i++) {
      excludePatterns.add(compileRegex(args[i], pc));
      System.out.println(args[i]);
    }

    final ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);
    data.setClassFinder(cf);

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

          //do not instrument itself
          //and do not instrument packages which are used during instrumented method invocation
          //(inside methods touch, save, etc from ProjectData)
          if (className.startsWith("com.intellij.rt")
           || className.startsWith("java.lang")
           || className.startsWith("java.util")
           || className.startsWith("java.io")
           || className.startsWith("java.text")
           || className.startsWith("gnu.trove")
           || className.startsWith("org.objectweb.asm")
           || className.startsWith("org.apache.oro.text.regex")) {
            return null;
          }

          final Perl5Matcher pm = new Perl5Matcher();

          // apply include and exclude patterns to parent class name only
          className = ClassNameUtil.getOuterClassName(className);
          for (Iterator it = excludePatterns.iterator(); it.hasNext();) {
            if (pm.matches(className, (Pattern)it.next())) return null;
          }

          cf.addClassLoader(loader);
          if (includePatterns.isEmpty() && loader != null) {
            return instrument(classfileBuffer, data);
          }
          for (Iterator it = includePatterns.iterator(); it.hasNext();) {
            if (pm.matches(className, (Pattern)it.next())) {
              return instrument(classfileBuffer, data);
            }
          }
        }
        catch (Throwable e) {
          ErrorReporter.reportError("Error during class instrumentation: " + className, e);
        }
        return null;
      }
    });
  }

  private static Pattern compileRegex(final String regex, final Perl5Compiler compiler) throws MalformedPatternException {
    // Perl5Compiler.READ_ONLY_MASK is required in case of multithreaded access to Pattern
    return compiler.compile(regex, Perl5Compiler.READ_ONLY_MASK);
  }

  private static byte[] instrument(final byte[] classfileBuffer, final ProjectData data) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    final ClassAdapter cv = data.isSampling() ? (ClassAdapter)new SamplingInstrumenter(data, cw) : new ClassInstrumenter(data, cw);
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
