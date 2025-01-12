package net.risesoft.service;

import java.util.Map;

import org.flowable.task.service.delegate.DelegateTask;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/30
 */
public interface PushNormalToAndroidService {

    /**
     * Description:
     * 
     * @param taskEntity 任务
     * @param variables 变量
     */
    void pushNormalToAndroid(final DelegateTask taskEntity, final Map<String, Object> variables);
}
