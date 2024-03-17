package org.agentTest;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


public class FilterInstrument extends ClassVisitor implements Opcodes {
    private String ClassName;

    public FilterInstrument(ClassVisitor classVisitor, String ClassName) {
        super(ASM6, classVisitor);
        this.ClassName = ClassName;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
//        System.out.println("java-agent !!!:  enter object " + ClassName);
        if (mv != null && ClassName.equals("java/io/ObjectInputStream") && name.equals("readOrdinaryObject")) {
            System.out.println("java-agent !!!:  find target method " + name + " " + descriptor + " " + signature);
            AddFilterAdapter at = new AddFilterAdapter(Opcodes.ASM4, mv);
            return at;
        }
        else if(mv != null && ClassName.equals("hudson/remoting/Capability") ){
//            System.out.println("java-agent !!!:  reject " + name + " " + descriptor + " " + signature);
            if(name.equals("read")){
                System.out.println("java-agent !!!:  find target method " + name + " " + descriptor + " " +
                        signature);
            }
            EnableChecker ec = new EnableChecker(Opcodes.ASM6, mv);
            return ec;
        }
        return mv;
    }


    public class AddFilterAdapter extends MethodVisitor{
        private int state;
        final static int SEEN_NOTHING = 0;
        final static int SEEN_FOR_CLASS = 1;
//        final static int SEEN_CONST= 2;
        final static int SEEN_ALOAD = 2;
        Set<String> policy = new HashSet<String>();

        public AddFilterAdapter(int api, MethodVisitor mv) {
            super(api, mv);
            try{
                System.out.println("java-agent !!!:  read file /home/zzzzzq/sda/java/agent-test/target/policy");
                BufferedReader fin = new BufferedReader(new FileReader("/home/zzzzzq/sda/java/agent-test/policy"));
                String str;
                while((str=fin.readLine())!=null){
                    System.out.println("java-agent !!!:  add white list " + str);
                    policy.add(str);
                }
                ReadObjectFilter.initPolicy(policy);
            }
            catch (IOException e){
                System.out.println("java-agent !!!:  read file error");
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                System.out.println("java-agent !!!:  init policy error");

                throw new RuntimeException(e);
            }
        }
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface){
            if(opcode == INVOKEVIRTUAL ){
                if(owner.equals("java/io/ObjectStreamClass") && name.equals("forClass")){
                    System.out.println("java-agent !!!:  find forClass invocation position");
                    state=SEEN_FOR_CLASS;
                }
            }
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
        @Override
        public void visitVarInsn(int opcode, int var) {

//            System.out.println("java-agent !!!:  to find astore in function " + opcode);
            if(state==SEEN_FOR_CLASS) {
//                System.out.println("java-agent !!!:  after for class invocation. " + opcode + " " + var);
                if (opcode == ASTORE) {
                    System.out.println("java-agent !!!:  find position");
                    state = SEEN_ALOAD;
                    mv.visitVarInsn(opcode, var);
                    mv.visitVarInsn(ALOAD, var);
                    mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReadObjectFilter.class), "checkPolicy", "(Ljava/lang/Class;)V", false);
                    System.out.println("java-agent !!!:  succ instrument invoke checker");

                    state=SEEN_NOTHING;
                    return;
                }
            }
            state = SEEN_NOTHING;
//            } else if (state == SEEN_CONST) {
//                System.out.println("java-agent !!!:  after const. " + opcode);
//                if(opcode==ASTORE){
//                    state = SEEN_ALOAD;
//                    System.out.println("java-agent !!!:  find position!!!");
////                    mv.visitMethodInsn(INVOKESTATIC, "org/agentTest/", "cost", "(Ljava/lang/String;)J", false);
//                    mv.visitVarInsn(var, opcode);
//                    return;
//                }
//
//            }
            mv.visitVarInsn(opcode, var);

        }
//        @Override
//        public void visitInsn(int opcode) {
//            state = SEEN_NOTHING;
//            mv.visitInsn(opcode);
//        }
    }




    public class EnableChecker extends MethodVisitor{

        Set<String> policy = new HashSet<String>();
        public EnableChecker(int api, MethodVisitor mv) {
            super(api, mv);
        }
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface){
            //java/io/ObjectInputStream.readObject ()Ljava/lang/Object;
            if(opcode == INVOKEVIRTUAL ){
                if(owner.equals("java/io/ObjectInputStream") && name.equals("readObject")){
                    System.out.println("java-agent !!!:  find readObject invocation position");
                    mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReadObjectFilter.class), "enablePolicy", "()V", false);
                    mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReadObjectFilter.class), "disablePolicy", "()V", false);
                    return;
                }
            }
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

    }



}


//    Field f=ClassLoader.class.getDeclaredField("classes");
//f.setAccessible(true);
//        Vector classes=(Vector)f.get(ClassLoader.getSystemClassLoader());
