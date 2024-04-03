package net.risesoft.listener;

import lombok.extern.slf4j.Slf4j;
import net.risesoft.pojo.Y9Result;
import net.risesoft.service.FlowableTenantInfoHolder;
import net.risesoft.y9.Y9Context;
import net.risesoft.y9.util.RemoteCallUtil;
import org.apache.commons.httpclient.NameValuePair;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.task.api.Task;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 提取
 *
 * @author qinman
 * @date 2024/03/19
 */
@Slf4j
public class PublishListener implements ExecutionListener {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notify(DelegateExecution execution) {
        try {
            String requestUrl = Y9Context.getProperty("y9.app.processAdmin.imageCountUrl") + "/service/rest/imageCount/publish";
            String tenantId = FlowableTenantInfoHolder.getTenantId();
            String processSerialNumber = execution.getVariable("processSerialNumber").toString();
            List<NameValuePair> params = new ArrayList<>();
            params.add(new NameValuePair("tenantId", tenantId));
            params.add(new NameValuePair("processSerialNumber", processSerialNumber));
            Y9Result<Boolean> y9Result = RemoteCallUtil.postCallRemoteService(requestUrl, params, Y9Result.class);
            if (!y9Result.isSuccess()) {
                throw new RuntimeException("调用发布接口失败：" + y9Result.getMsg());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用发布接口异常");
        }
    }
}