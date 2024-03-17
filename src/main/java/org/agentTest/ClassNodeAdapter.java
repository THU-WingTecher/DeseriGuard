package org.agentTest;

//import javafx.util.Pair;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.objectweb.asm.Opcodes.*;

public class ClassNodeAdapter  extends  ClassNode{
    static Map<String, Map<String, Map<Integer, RestrictionPolicy>>> InjectSwitcherObjects;

    static Map<String, Map<String, List<RestrictionPolicy>>> InjectCheckerObjects;
    private String className;
    private Map<String, Map<Integer, RestrictionPolicy>> curSwitcherPolicy;

    private Map<String, List<RestrictionPolicy>> curCheckerPolicy;

    static Map<String, Class> classCache = new ConcurrentHashMap<>();
//    static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    static Set<String> unknownClasses = new HashSet<>();
    boolean isChecker;

    public ClassNodeAdapter(ClassVisitor cv, String className, boolean isChecker) {
        super(ASM6);
        this.cv = cv;
        this.className = className;
        this.isChecker = isChecker;
        if(isChecker){
            curCheckerPolicy = InjectCheckerObjects.get(className);
        }
        else{
            curSwitcherPolicy = InjectSwitcherObjects.get(className);
        }
    }

    @Override
    public void visitEnd() {
        if(isChecker){
//            InjectPolicyField();
            InjectChecker();
        }
        else{
            InjectSwitcher();
        }

        if(cv!=null){
            accept(cv);
        }
    }

    private void InjectPolicyField(){
        for(FieldNode fn:fields){
            if(fn.name.equals("DeserializePolicy")){
                return;
            }
        }
        int cnt=0;
        List<List<String>> policyList = new ArrayList<>();
        for(Map.Entry<String, List<RestrictionPolicy>> entry: curCheckerPolicy.entrySet()){
            for(RestrictionPolicy curPolicy : entry.getValue()){
                curPolicy.index = cnt;
                cnt++;
                policyList.add( curPolicy.policy);

            }
        }
//        List<String>[] arr = new ArrayList<String>[policyList.size()];
//        List<String>[] policyArray = policyList.toArray(arr);
//        policyArray = (List<String>[]) policyList.toArray();
        System.out.println(Type.getDescriptor(policyList.get(0).getClass()));

        this.fields.add(new FieldNode(ACC_PRIVATE+ACC_STATIC, "DeserializePolicy", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", policyList.get(0)));
        Iterator<MethodNode> methodIterator = methods.iterator();
        while(methodIterator.hasNext()){
            MethodNode mn = methodIterator.next();
            if(!curCheckerPolicy.equals("<clinit>")){
                continue;
            }
            System.out.println("java-agent !!!:  find <clinit> function at " );


        }
        System.out.println("java-agent !!!:  add field DeserializePolicy success");
    }


    private void InjectChecker(){
        Iterator<MethodNode> methodIterator = methods.iterator();
        System.out.println("java-agent !!!:  starting inject checker at " + className);


        while(methodIterator.hasNext()){
            MethodNode mn = methodIterator.next();
            if(!curCheckerPolicy.containsKey(mn.name)){
                continue;
            }
            System.out.println("java-agent !!!:  find checker's function at " + mn.name);

            RestrictionPolicy curPolicy = curCheckerPolicy.get(mn.name).get(0); // TODO: 2023/3/25 optimize structure of RestrictionPolicy
            InsnList insns = mn.instructions;
            Iterator<AbstractInsnNode> i = insns.iterator();
            int count = 0;
            AbstractInsnNode instrumentInstr=null;
            while (i.hasNext()) {
                AbstractInsnNode in = i.next();


//                    if(in.getOpcode() == INVOKESTATIC || in.getOpcode() == INVOKEVIRTUAL || in.getOpcode() == INVOKESPECIAL || in.getOpcode() == INVOKEINTERFACE){
                if (in instanceof  MethodInsnNode){
                    MethodInsnNode invoke= (MethodInsnNode)in;
                    String method = invoke.name;
                    String object = invoke.owner;
                    if(method.equals(curPolicy.forClassFunction) && object.equals(curPolicy.forClassObject)){
                        count+=1;
                        if(count==curPolicy.invokeCount){
                            System.out.println("java-agent !!!:  searching for deserialize object " + object + ":" +method +"----- target is " + curPolicy.forClassObject + ":" +curPolicy.forClassFunction);
                            System.out.println("java-agent !!!:  reach a position for call function for " + count + " times");
                            instrumentInstr = in;
                            break;
                        }
                    }


                }
//                    else(in.getOpcode() == INVOKEVIRTUAL){
//
//                    }
            }
            System.out.println("java-agent !!!: run out of instruction search, instr is " + (instrumentInstr == null ? "null" : "right"));
            if(instrumentInstr!=null){
                InsnList il = new InsnList();
                il.add(new LdcInsnNode(className));
                il.add(new LdcInsnNode(mn.name));
                il.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(ReadObjectFilter.class), "checkPolicy", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;", false));
//                mn.instructions.insert(instrumentInstr, new MethodInsnNode(INVOKESTATIC, Type.getInternalName(ReadObjectFilter.class), "checkPolicy", "(Ljava/lang/Class;)Ljava/lang/Class;", false));
//                mn.instructions.insert(instrumentInstr, new )
                mn.instructions.insert(instrumentInstr, il);
                System.out.println("java-agent !!!:  find position and finish checker instrument");
            }


        }
    }



    private  void InjectSwitcher(){
        Iterator<MethodNode> methodIterator = methods.iterator();
        System.out.println("java-agent !!!:  starting inject switcher at " + className);
        while(methodIterator.hasNext()){
            MethodNode mn = methodIterator.next();
//            System.out.println("java-agent !!!:  pass function " + mn.name);
            if(!curSwitcherPolicy.containsKey(mn.name)){
                continue;
            }
//            if (mn.name.equals("<init>"){
//                Iterator<ParameterNode> paraIter = mn.parameters.iterator();
//                while (paraIter.hasNext()){
//                    ParameterNode para = paraIter.next();
//                    if
//                }
//            }
            System.out.println("java-agent !!!:  find switcher's function at " + mn.name);
            Map<Integer, RestrictionPolicy> curPolicyMap = curSwitcherPolicy.get(mn.name);
            InsnList insns = mn.instructions;
            Iterator<AbstractInsnNode> i = insns.iterator();
            int lineNumber = 0;
            List<AbstractInsnNode> instrList = new ArrayList<>();
            List<Integer> lineList = new ArrayList<>();
            while (i.hasNext()) {
                AbstractInsnNode in = i.next();
                if(in instanceof LineNumberNode){
                    lineNumber = ((LineNumberNode) in).line;
                }
//                System.out.println("java-agent !!!:  current linenumber " + lineNumber);
                if(curPolicyMap.containsKey(lineNumber)){
                    RestrictionPolicy curPolicy = curPolicyMap.get(lineNumber);
                    try {
//                        if (in.getOpcode() == INVOKESTATIC || in.getOpcode() == INVOKEVIRTUAL || in.getOpcode() == INVOKESPECIAL || in.getOpcode() == INVOKEINTERFACE) {
                        if (in instanceof  MethodInsnNode){
                            MethodInsnNode invoke= (MethodInsnNode)in;
                            String method = invoke.name;
                            String object = invoke.owner;
                            System.out.println("java-agent !!!:  searching for deserialize object " + object + ":" +method +"----- target is " + curPolicy.deserObject + ":" +curPolicy.deserMethod);
                            if(method.equals(curPolicy.deserMethod) & object.equals(curPolicy.deserObject)){
                                System.out.println("java-agent !!!:  reach a linenumber " + lineNumber + " and invoke function");
                                instrList.add(in);
                                lineList.add(curPolicyMap.get(lineNumber).index);
                            }
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
//            System.out.println("java-agent !!!:  run out of loop\n");
            for(int j=0; j < lineList.size(); j++){
                AbstractInsnNode inst = instrList.get(j);
                System.out.println("java-agent !!!:  insturment at inst " + inst + " for index " + lineList.get(j));
                mn.instructions.insertBefore(inst, new IntInsnNode(BIPUSH, lineList.get(j)));
                mn.instructions.insertBefore(inst, new MethodInsnNode(INVOKESTATIC, "org/agentTest/ReadObjectFilter", "enablePolicy", "(I)V", false));
                inst = inst.getNext();
                mn.instructions.insertBefore(inst, new MethodInsnNode(INVOKESTATIC, "org/agentTest/ReadObjectFilter", "dumpObject", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                mn.instructions.insertBefore(inst, new MethodInsnNode(INVOKESTATIC, "org/agentTest/ReadObjectFilter", "disablePolicy", "()V", false));
            }

        }

    }

//    public void checkMethod(AbstractInsnNode inst, String targetObject, String targetMethod){
//        String method;
//        String object;
//        switch (inst.getOpcode()){
//            case INVOKESTATIC:
//                MethodInsnNode invoke= (MethodInsnNode)inst;
//                String method = invoke.name;
//                String object = invoke.owner;
//                if()


//        }
//    }
    public static Class loadClass(ClassLoader loader, String ClassName){
//        return cacheClass(loader, ClassName); //move here for resolve deadlock

        if (classCache.containsKey(ClassName)){
            Class res = classCache.get(ClassName);
            return res;
        }

        return cacheClass(loader, ClassName);
    }

    public static Class cacheClass(ClassLoader loader, String ClassName){
        Class<?> target = null;
        if(loader == null){
            target = nativeLoader(ClassName);
        }
        else{
            try{
                target = loader.loadClass(ClassName);
            }
            catch (Exception e1) {
                target = nativeLoader(ClassName);
            }

        }
        if(target != null){
//            System.out.println("java-agent !!!:  cache class: " + target.getName());
//            classCache.put(ClassName, target);  //resolve deadlock
        }
        return target;
    }

    private static  Class nativeLoader(String ClassName){
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class target=null;
        if(cl ==null){
            cl = ClassLoader.getSystemClassLoader();
            if(cl == null){
                return null;
            }
            try {
                target = cl.loadClass(ClassName);
            } catch (Exception e1) {
                return null;
            }
        }
        else {
            try {
                target = cl.loadClass(ClassName);
            } catch (Exception e1) {
                cl = ClassLoader.getSystemClassLoader();
                if(cl == null){
                    return null;
                }
                try {
                    target = cl.loadClass(ClassName);
                } catch (Exception e2) {
                    return null;
                }
            }
        }
        return target;
    }

    public static void loadPolicy(String filePath) throws FileNotFoundException {
        System.out.println(filePath);
        BufferedReader fin = new BufferedReader(new FileReader(filePath));
        String str;
        RestrictionPolicy restrictionPolicy=null;
        InjectSwitcherObjects = new HashMap<>();
        InjectCheckerObjects = new HashMap<>();

        try {
            while((str=fin.readLine())!=null){
                if(str.contains(":")){
                    restrictionPolicy = new RestrictionPolicy(str);
                    if(!InjectSwitcherObjects.containsKey(restrictionPolicy.object)){
                        InjectSwitcherObjects.put(restrictionPolicy.object, new HashMap<String, Map<Integer, RestrictionPolicy>>());
                    }
                    if(!InjectSwitcherObjects.get(restrictionPolicy.object).containsKey(restrictionPolicy.function)){
                        InjectSwitcherObjects.get(restrictionPolicy.object).put(restrictionPolicy.function, new HashMap<Integer, RestrictionPolicy>());
                    }
//                    System.out.println("java-agent !!!:  why" + (restrictionPolicy.object==null) + (InjectSwitcherObjects.get(restrictionPolicy.object) == null));
                    InjectSwitcherObjects.get(restrictionPolicy.object).get(restrictionPolicy.function).put(restrictionPolicy.line, restrictionPolicy);
                    String checkerObject = restrictionPolicy.checkerObject;
                    String checkMethod = restrictionPolicy.checkerFunction;
                    if(!InjectCheckerObjects.containsKey(checkerObject)){
                        InjectCheckerObjects.put(checkerObject, new HashMap<String, List<RestrictionPolicy>>());
                        System.out.println("java-agent !!!:  put a checker object " + checkerObject);
                    }
                    if(!InjectCheckerObjects.get(checkerObject).containsKey(checkMethod)){
                        InjectCheckerObjects.get(checkerObject).put(checkMethod, new ArrayList<RestrictionPolicy>());
                    }

                    int index = InjectCheckerObjects.get(checkerObject).get(checkMethod).size();
                    restrictionPolicy.index = index;
                    InjectCheckerObjects.get(checkerObject).get(checkMethod).add(restrictionPolicy);
                    continue;
                }
                assert restrictionPolicy!=null;
                String className = str.replace("/", ".");
                restrictionPolicy.addPolicy(className);
//                System.out.println("java-agent !!!:  add unknown class :" + str);
                unknownClasses.add(str);
//                System.out.println("java-agent !!!:  add white list " + str);
//                Class loadedClass = cacheClass(null, className);
//                if(loadedClass != null){
//                    restrictionPolicy.addPermitted(loadedClass);
//                }
//                else{
//                    lock.writeLock().lock();
//                    unknownClasses.add(className);
//                    lock.writeLock().unlock();
//
//                }


            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void chacheAClass(String name, Class cls){
//        System.out.println("java-agent !!!:  check unknown class :" + name);
        if(cls!=null && ClassNodeAdapter.unknownClasses.contains(name)){
            ClassNodeAdapter.unknownClasses.remove(name);
            ClassNodeAdapter.classCache.put(name, cls);
            System.out.println("java-agent !!!:  add cache :" + name);
        }
  }



}
