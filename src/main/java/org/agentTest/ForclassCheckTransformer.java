package org.agentTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

//import java.io.IOException;
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

public class ForclassCheckTransformer implements ClassFileTransformer {
    private boolean treeMode = false;
    public ForclassCheckTransformer(String policyPath){
        try {
            ClassNodeAdapter.loadPolicy(policyPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        File directory = new File("/tmp/log/");
        if (!directory.exists()) {
            directory.mkdir();
        }

        treeMode = true;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
//        System.out.println("java-agent !!!:  what happens " +className);
        if(className.startsWith("java.util.concurrent.")){
            return null;
        }
//        ClassNodeAdapter.chacheAClass(className, classBeingRedefined); // resolve deadlock
        if(treeMode){
            if(ClassNodeAdapter.InjectCheckerObjects.containsKey(className)) {
                System.out.println("java-agent !!!:  process checker class " + className + " " + loader);
                try {
                    ClassReader cr = new ClassReader(classfileBuffer);
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                    ClassNodeAdapter classNode = new ClassNodeAdapter(cw, className, true);
                    cr.accept(classNode, ClassReader.EXPAND_FRAMES);
                    System.out.println("java-agent !!!:  after instrument checker");


                    ClassReader printCR = new ClassReader(cw.toByteArray());

                    FileOutputStream fileOutputStream = new FileOutputStream(new File("/tmp/log/check.bytecode"));
                    TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(fileOutputStream));
                    printCR.accept(tcv, 0);
                    fileOutputStream.close();

                    return cw.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            else if(ClassNodeAdapter.InjectSwitcherObjects.containsKey(className)){
                System.out.println("java-agent !!!:  process switcher class " + className + " " + loader);
                try {
                    URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                    Class sysclass = URLClassLoader.class;
                    try {
                        Class[] parameters = new Class[]{URL.class};
                        Method method = sysclass.getDeclaredMethod("addURL", parameters);
                        method.setAccessible(true);
                        URL url = new URL("file:/home/zzzzzq/sda/java/vulnerability/olingo-odata4/ext/client-proxy/target/odata-client-proxy-4.6.0.jar");
                        method.invoke(sysloader, new Object[]{url});
                        url = new URL("file:/home/zzzzzq/.m2/repository/org/jenkins-ci/main/remoting/2.52/remoting-2.52.jar");
                        method.invoke(sysloader, new Object[]{url});
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new IOException("Error, could not add URL to system classloader");
                    }

                    ClassReader cr = new ClassReader(classfileBuffer);
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                    ClassNodeAdapter classNode = new ClassNodeAdapter(cw, className, false);
                    cr.accept(classNode, ClassReader.EXPAND_FRAMES);




                    ClassReader printCR = new ClassReader(cw.toByteArray());
                    FileOutputStream fileOutputStream = new FileOutputStream(new File("/tmp/log/switcher.bytecode"));
                    TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(fileOutputStream));
                    printCR.accept(tcv, 0);
                    fileOutputStream.close();

                    return cw.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
//                    System.out.println(e);
                    throw new RuntimeException(e);
                }

            }
        }
        else {
//            if (!className.equals("java/io/ObjectInputStream") && !className.equals("hudson/remoting/Capability")) {
////            System.out.println("java-agent !!!:  reject class " +className);
//                return null;
//            }
            try {
                System.out.println("java-agent !!!:  loaded class " + className + " " + loader);
                ClassReader cr = new ClassReader(classfileBuffer);
                System.out.println("java-agent !!!:  loaded class after" + className);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);

//            if(className.equals("hudson/remoting/Capability")){
//                cw = new ClassWriter(cr, 0);
//            }
//            else{
//            cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
//            }
            cr.accept(new FilterInstrument(cw, className), ClassReader.EXPAND_FRAMES);
                return cw.toByteArray();
            } catch (Exception e) {
//                System.out.print(e);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
//            ClassReader printCR = new ClassReader(cw.toByteArray());
//            TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
//            printCR.accept(tcv, 0);
        }
        return null;
    }
}
