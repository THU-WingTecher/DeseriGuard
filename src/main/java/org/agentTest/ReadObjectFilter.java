package org.agentTest;

//import java.lang.reflect.Field;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
//import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import java.util.Vector;
//import org.objectweb.asm.Type;

public class ReadObjectFilter {
    private static Set<String> policy=null;
    private static  BufferedWriter reject;

    static {
        try {
            reject = new BufferedWriter(new FileWriter("/tmp/log/reject.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static long dumpCount = 0;
//    private static long totalTime = 0;
    private  static  Integer checkInitValue = new Integer(-1);
    public static  ThreadLocal<Integer> isCheck = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return checkInitValue;
        }
    };
    public static  void initPolicy(Set<String> newPolicy) throws ClassNotFoundException {
        if(policy==null){
//            policy = new HashSet<Class<?> >();
//            for(String typeStr : newPolicy){
//                System.out.println("java-agent !!!:  find type " + typeStr);
//                policy.add(Class.forName(typeStr));
//            }
            policy = newPolicy;
        }
    }

    public  static void enablePolicy(int index){
        long current = System.nanoTime();
        isCheck.set(new Integer(index));
        long end = System.nanoTime();
        System.out.println("java-agent !!!:  checker is enabled in " + (end-current) + " for index " + index);


    }

    public  static void disablePolicy(){
        long current = System.nanoTime();
        isCheck.set(checkInitValue);
        long end = System.nanoTime();
        System.out.println("java-agent !!!:  checker is disabled in " + (end-current));
    }

    public static Object dumpObject(Object obj){

        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("/home/zzzzzq/sda/java/experiment/dumpObjects/ofbiz_try/"+dumpCount + "-" + isCheck.get() +".obj"));
            oos.writeObject(obj);
            oos.close();
        } catch (IOException e) {
            System.out.println("java-agent !!!:  fail to dump object " + obj.toString());
            dumpCount++;
            return obj;
        }
        dumpCount++;

        return obj;
    }

    public  static Class<?> checkPolicy(Class<?>  cls, String object, String method){
//        System.out.println("java-agent !!!:  in check Policy")
        if(isCheck.get() == -1 || cls == null){
//            System.out.println("java-agent !!!:  policy check is not enabled");
            return cls;
//            try {
//
//                throw new RuntimeException();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//                return cls;
//            }
        }

        long current = System.nanoTime();
        Class<?> retClass = cls;
        if(cls.isAnonymousClass()){
            cls = cls.getInterfaces().length == 0 ? cls.getSuperclass() : cls.getInterfaces()[0];
        }

        if(cls.isPrimitive() | cls.isEnum() | cls.getName().equals("java.lang.String") | cls.getName().equals("java.lang.StackTraceElement") | cls.getName().equals("java.util.Collections$UnmodifiableList") || cls.getName().equals("java.util.ArrayList") | cls.getName().startsWith("com.sun.proxy.$Proxy") | Exception.class.isAssignableFrom(cls)){
            return retClass;
        }

//        try {

//            Field f = ClassLoader.class.getDeclaredField("classes");
//            f.setAccessible(true);
//            Vector classes = (Vector<Class>) f.get(ClassLoader.getSystemClassLoader());
//            for(int i=0;i<classes.size();i++){
//                System.out.println(classes.get(i));
//            }
//        }
//        catch (NoSuchFieldException e) {
//            throw new RuntimeException(e);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("java-agent !!!:  Start to Check");
        RestrictionPolicy policy  = ClassNodeAdapter.InjectCheckerObjects.get(object).get(method).get(isCheck.get());
        System.out.println("java-agent !!!:  check " + object +"'s policy, checking "+ cls.getName() +" " + cls.getClassLoader());

        if(policy.permittedClass.contains(cls)){
            long end = System.nanoTime();
            System.out.println("java-agent !!!:  permit deserialize " + cls.getName() +" according to rule " + cls.getName() + ". Check finish in " + (end-current));
            return retClass;
        }
        System.out.println("java-agent !!!: Current policy contains " + policy.policy.size() + " rules");
        for(String typeStr : policy.policy){
//                Class<?> targetCls = ClassLoader.getSystemClassLoader().loadClass("/home/zzzzzq/.jenkins/war/WEB-INF/lib/remoting-2.52.jar");
//                AppClassLoader.getAppClassLoader(extcl);
            Class<?> targetCls = null;// Class.forName(, true, );
//            try {
                targetCls = ClassNodeAdapter.loadClass(cls.getClassLoader(), typeStr);


//                if (typeStr.contains("UserRequest")){
//                    policy.addPermitted(targetCls.getName());
//                }
                if(targetCls == null){
//                    System.out.println("java-agent !!!:  fail to find the class of " + typeStr);
                    continue;
                }
//                if(cls.getName().contains("Request")){
//                    System.out.println("java-agent !!!:  found deserilize of " + targetCls.getName() + " and " + cls.getName());
//                    System.out.println("java-agent !!!:  why not assgnable?" + (targetCls.isAssignableFrom(cls)) + " or " +  (cls.isAssignableFrom(targetCls)));
//                }
                if(targetCls.isAssignableFrom(cls)){
                    policy.addPermitted(cls.getName());
                    long end = System.nanoTime();
                    System.out.println("java-agent !!!:  permit deserialize " + cls.getName() + " according to rule " + targetCls.getName() + ". Check finish in " + (end-current));
                    return retClass;
                }
//            }
//            catch (ClassNotFoundException e) {
//                    disablePolicy();
//            throw new RuntimeException(e);
//            }
        }
        long end = System.nanoTime();
        System.out.println("java-agent !!!:  reject deserialize " + cls.getName() + ". check finish in " + (end-current));
        try {
            reject.write("java-agent !!!:  reject deserialize " + cls.getName() + " for policy index " + policy.index + "\n");
            reject.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        disablePolicy();
        throw new RuntimeException();
//        return retClass;
    }
}
