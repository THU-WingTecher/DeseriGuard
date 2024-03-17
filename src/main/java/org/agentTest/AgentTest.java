package org.agentTest;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import org.agentTest.ForclassCheckTransformer;
/**
 * Java Agent Test!
 *
 */
public class AgentTest
{

    public static void premain(String agentArgs, Instrumentation inst){
        long start = System.nanoTime();
        System.out.println("java-agent !!!:  start instrument java agent");
        final ForclassCheckTransformer  t = new ForclassCheckTransformer(agentArgs);
        inst.addTransformer(t);
        long end = System.nanoTime();
        System.out.println("java-agent !!!:  agent init finish in " + (end-start));

//        Class[] classes = inst.getAllLoadedClasses();
//        List<Class> candidates = new ArrayList<Class>();
//        for (Class c : classes) {
//            if (inst.isModifiableClass(c) && inst.isRetransformClassesSupported()){
//               System.out.println(c.getName());
//            }
//        }
    }
//    public static void agentmain(String agentArgs, )

}
