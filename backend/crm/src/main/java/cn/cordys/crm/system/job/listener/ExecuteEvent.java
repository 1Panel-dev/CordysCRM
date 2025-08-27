package cn.cordys.crm.system.job.listener;

import org.springframework.context.ApplicationEvent;

public class ExecuteEvent extends ApplicationEvent {
    public ExecuteEvent(Object source) {
        super(source);
    }
}
