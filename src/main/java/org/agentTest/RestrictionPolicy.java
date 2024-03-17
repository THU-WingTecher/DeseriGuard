package org.agentTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RestrictionPolicy {
    public int line;
    public String file;
    public String function;
    public String object;
    public List<String> policy = new ArrayList<String>();
    public Set<String> permittedClass = new HashSet<>();
    public String deserMethod;
    public String deserObject;
    public String checkerObject;
    public String checkerFunction;
    public int invokeCount;
    public String forClassFunction;
    public String forClassObject;

    public long totaltime;
    public int index;

    public RestrictionPolicy(String str){
        String[] list = str.split("->");
        String[] checkList = list[2].split(":");
        String[] deserList = list[1].split(":");
        String[] forClassList = list[3].split(":");
        this.deserMethod = deserList[1];
        this.deserObject = deserList[0];
        this.checkerObject = checkList[0];
        this.checkerFunction = checkList[1];
        this.invokeCount = Integer.parseInt(checkList[2]);
        this.forClassFunction = forClassList[1];
        this.forClassObject = forClassList[0];

        list = list[0].split(":");
        file = list[0];
        line = Integer.parseInt(list[3]);
        function = list[2];
        object = list[1];
//        System.out.println(this);
    }

    @Override
    public String toString() {
        return "file"+":"+file +"\n" +
                "object"+":"+object +"\n" +
                "function"+":"+function +"\n" +
                "line"+":"+line +"\n" +
                "------------------------\n"+
                "deserObject"+":"+deserObject +"\n" +
                "deserMethod"+":"+deserMethod +"\n" +
                "------------------------\n"+
                "checkerObject"+":"+checkerObject +"\n" +
                "checkerFunction"+":"+checkerFunction +"\n" +
                "checkline"+":"+invokeCount +"\n" +
                "------------------------\n"+
                "forClassObject"+":"+forClassObject +"\n" +
                "forClassFunction"+":"+forClassFunction +"\n";
    }

    public void addPolicy(String str){
        policy.add(str);
    }

    public void addPermitted(String className) {permittedClass.add(className);}
}
