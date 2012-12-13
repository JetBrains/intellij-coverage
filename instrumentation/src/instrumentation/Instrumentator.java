package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.ClassWriter;
import org.jetbrains.asm4.Opcodes;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;


public class Instrumentator {

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    String[] args;
    File argsFile = new File(argsString);
    if (argsFile.isFile()) {
      try {
        args = readArgsFromFile(argsString);
      } catch (IOException e) {
        ErrorReporter.reportError("Arguments were not passed correctly", e);
        return;
      }
    } else {
      args = tokenize(argsString);
    }

    final boolean traceLines = Boolean.valueOf(args[1]).booleanValue();
    final boolean sampling = Boolean.valueOf(args[4]).booleanValue();
    final File dataFile = new File(args[0]);
    final boolean calcUnloaded = Boolean.valueOf(args[2]).booleanValue();
    ProjectData initialData = null;
    if (Boolean.valueOf(args[3]).booleanValue() && dataFile.isFile()) {
      initialData = ProjectDataLoader.load(dataFile);
    }
    final ProjectData data = ProjectData.createProjectData(dataFile, initialData, traceLines, sampling);
    final List includePatterns = new ArrayList();
    System.out.println("---- IntelliJ IDEA coverage runner ---- ");
    System.out.println(sampling ? "sampling ..." : ("tracing " + (traceLines ? "and tracking per test coverage ..." : "...")));
    final String excludes = "-exclude";
    int i = 5;
    System.out.println("include patterns:");
    for (; i < args.length; i++) {
      if (excludes.equals(args[i])) break;
      includePatterns.add(Pattern.compile(args[i]));
      System.out.println(args[i]);
    }
    System.out.println("exclude patterns:");
    i++;
    final List excludePatterns = new ArrayList();
    for (; i < args.length; i++) {
      excludePatterns.add(Pattern.compile(args[i]));
      System.out.println(args[i]);
    }

    final ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);
    Runtime.getRuntime().addShutdownHook(new Thread(new SaveHook(dataFile, calcUnloaded, cf)));

    instrumentation.addTransformer(new ClassFileTransformer() {
      private boolean computeFrames = computeFrames();

      public byte[] transform(ClassLoader loader,
                              String className,
                              Class classBeingRedefined,
                              ProtectionDomain protectionDomain,
                              byte[] classfileBuffer) throws IllegalClassFormatException {
        if (data.isStopped()) return null;
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
          if (className.startsWith("com.intellij.rt.")
            || className.startsWith("java.")
            || className.startsWith("sun.")
            || className.startsWith("gnu.trove.")
            || className.startsWith("org.jetbrains.asm4.")
            || className.startsWith("org.apache.oro.text.regex.")) {
            return null;
          }

          // apply include and exclude patterns to parent class name only
          String outerClassName = ClassNameUtil.getOuterClassName(className);
          for (Iterator it = excludePatterns.iterator(); it.hasNext(); ) {
            if (((Pattern) it.next()).matcher((outerClassName)).matches()) return null;
          }

          cf.addClassLoader(loader);
          if (includePatterns.isEmpty() && loader != null) {
            return instrument(classfileBuffer, data, className, loader, computeFrames);
          }
          for (Iterator it = includePatterns.iterator(); it.hasNext(); ) {
            if (((Pattern) it.next()).matcher(className).matches()) {
              return instrument(classfileBuffer, data, className, loader, computeFrames);
            }
          }
        } catch (Throwable e) {
          ErrorReporter.reportError("Error during class instrumentation: " + className, e);
        }
        return null;
      }

      private boolean computeFrames() {
        return System.getProperty("idea.coverage.no.frames") == null;
      }
    });
  }

  private static String[] readArgsFromFile(String arg) throws IOException {
    final List result = new ArrayList();
    final File file = new File(arg);
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
      while (reader.ready()) {
        result.add(reader.readLine());
      }
    } finally {
      reader.close();
    }
    return (String[]) result.toArray(new String[result.size()]);
  }

  private static byte[] instrument(final byte[] classfileBuffer, final ProjectData data, String className, ClassLoader loader, boolean computeFrames) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw;
    if (computeFrames) {
      final int version = getClassFileVersion(cr);
      cw = getClassWriter(version >= Opcodes.V1_6 && version != Opcodes.V1_1 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS, loader);
    } else {
      cw = getClassWriter(ClassWriter.COMPUTE_MAXS, loader);
    }
    final ClassVisitor cv = data.isSampling() ? ((ClassVisitor) new SamplingInstrumenter(data, cw, className)) : new ClassInstrumenter(data, cw, className);
    cr.accept(cv, 0);
    return cw.toByteArray();
  }

  private static ClassWriter getClassWriter(int flags, final ClassLoader classLoader) {
    /*if (flags == ClassWriter.COMPUTE_FRAMES && System.getProperty("idea.asm.default.compute.frames") == null) {

      final Class classFinder;
      try {
        classFinder = Class.forName("com.intellij.compiler.instrumentation.InstrumentationClassFinder");
      } catch (ClassNotFoundException e) {
        //do not log error when finder is not supported
        return new MyClassWriter(flags, classLoader);
      }

      try {
        Constructor constructor = classFinder.getDeclaredConstructor(new Class[]{new URL[0].getClass(), new URL[0].getClass()});
        if (constructor != null) {
          constructor.setAccessible(true);
          if (classLoader instanceof URLClassLoader) {
            final URL[] urls = ((URLClassLoader) classLoader).getURLs();
            final ClassLoader parentLoader = classLoader.getParent();
            URL[] platform = null;
            if (parentLoader == null) {
              platform = new URL[0];
            } else if (parentLoader instanceof URLClassLoader) {
              platform = ((URLClassLoader) parentLoader).getURLs();
            }
            if (platform != null) {
              final Object finder = constructor.newInstance(new Object[]{platform, urls});
              final Class classWriter = Class.forName("com.intellij.compiler.instrumentation.InstrumenterClassWriter");
              final Constructor classWriterDeclaredConstructor = classWriter.getDeclaredConstructor(new Class[]{int.class, classFinder});
              if (classWriterDeclaredConstructor != null) {
                classWriterDeclaredConstructor.setAccessible(true);
                return (ClassWriter) classWriterDeclaredConstructor.newInstance(new Object[]{Integer.valueOf(flags), finder});
              }
            }
          }
        }
      } catch (Exception e) {
        ErrorReporter.logError(e.getMessage());
      }
    }
*/
    return new MyClassWriter(flags, classLoader);
  }

  public static int getClassFileVersion(ClassReader reader) {
    final int[] classFileVersion = new int[1];
    reader.accept(new ClassVisitor(Opcodes.ASM4) {
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classFileVersion[0] = version;
      }
    }, 0);
    return classFileVersion[0];
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

  private static class MyClassWriter extends ClassWriter {
    public static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private final ClassLoader classLoader;

    public MyClassWriter(int flags, ClassLoader classLoader) {
      super(flags);
      this.classLoader = classLoader;
    }

    protected String getCommonSuperClass(String type1, String type2) {
      try {
        ClassReader info1 = typeInfo(type1);
        ClassReader info2 = typeInfo(type2);
        String 
        superType = checkImplementInterface(type1, type2, info1, info2);
        if (superType != null) return superType;
        superType = checkImplementInterface(type2, type1, info2, info1);
        if (superType != null) return superType;

        StringBuilder b1 = typeAncestors(type1, info1);
        StringBuilder b2 = typeAncestors(type2, info2);
        String result = JAVA_LANG_OBJECT;
        int end1 = b1.length();
        int end2 = b2.length();
        while (true) {
          int start1 = b1.lastIndexOf(";", end1 - 1);
          int start2 = b2.lastIndexOf(";", end2 - 1);
          if (start1 != -1 && start2 != -1 && end1 - start1 == end2 - start2) {
            String p1 = b1.substring(start1 + 1, end1);
            String p2 = b2.substring(start2 + 1, end2);
            if (p1.equals(p2)) {
              result = p1;
              end1 = start1;
              end2 = start2;
            } else {
              return result;
            }
          } else {
            return result;
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e.toString());
      }
    }

    private String checkImplementInterface(String type1, String type2, ClassReader info1, ClassReader info2) throws IOException {
      if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
        if (typeImplements(type2, info2, type1)) {
          return type1;
        }
        return JAVA_LANG_OBJECT;
      }
      return null;
    }

    private StringBuilder typeAncestors(String type, ClassReader info) throws IOException {
      StringBuilder b = new StringBuilder();
      while (!JAVA_LANG_OBJECT.equals(type)) {
        b.append(';').append(type);
        type = info.getSuperName();
        info = typeInfo(type);
      }
      return b;
    }

    private boolean typeImplements(String type, ClassReader classReader, String interfaceName) throws IOException {
      while (!JAVA_LANG_OBJECT.equals(type)) {
        String[] itfs = classReader.getInterfaces();
        for (int i = 0; i < itfs.length; ++i) {
          if (itfs[i].equals(interfaceName)) {
            return true;
          }
        }
        for (int i = 0; i < itfs.length; ++i) {
          if (typeImplements(itfs[i], typeInfo(itfs[i]), interfaceName)) {
            return true;
          }
        }
        type = classReader.getSuperName();
        classReader = typeInfo(type);
      }
      return false;
    }

    private ClassReader typeInfo(final String type) throws IOException {
      InputStream is = classLoader.getResourceAsStream(type + ".class");
      try {
        return new ClassReader(is);
      } finally {
        is.close();
      }
    }
  }
}
