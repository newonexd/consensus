package org.newonexd.raft.common.msg;

import java.io.Serializable;

public class RpcMessage extends MessageBody implements Serializable {

    public RpcMessage(String className, String methodName, Object[] arguments) {
        this.className = className;
        this.methodName = methodName;
        this.arguments = arguments;
    }


    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    private String className;


    private String methodName;

    private Object[] arguments;

}
