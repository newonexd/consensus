package org.newonexd.raft.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


@Component
public final class RpcApplication implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RpcApplication.applicationContext = applicationContext;
    }

    public static Object getComponentByClassName(String className){
        return applicationContext.getBean(className);
    }
}
