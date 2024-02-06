package net.risesoft.api.itemadmin;

import java.util.List;
import java.util.Map;

import net.risesoft.model.itemadmin.OpinionHistoryModel;
import net.risesoft.model.itemadmin.OpinionModel;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/19
 */
public interface OpinionApi {

    /**
     * 检查当前taskId任务节点是否已经签写意见
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param processSerialNumber 流程编号
     * @param taskId 任务id
     * @return Boolean
     */
    Boolean checkSignOpinion(String tenantId, String userId, String processSerialNumber, String taskId);

    /**
     * 获取意见框历史记录数量
     *
     * @param tenantId 租户id
     * @param processSerialNumber 流程编号
     * @param opinionFrameMark 意见框Id
     * @return int
     */
    int countOpinionHistory(String tenantId, String processSerialNumber, String opinionFrameMark);

    /**
     * 删除意见
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param id 唯一标识
     * @throws Exception Exception
     */
    void delete(String tenantId, String userId, String id) throws Exception;

    /**
     * 获取事项绑定的意见框列表
     *
     * @param tenantId 租户id
     * @param itemId 事项id
     * @param processDefinitionId 流程定义Id
     * @return List&lt;String&gt;
     */
    List<String> getBindOpinionFrame(String tenantId, String itemId, String processDefinitionId);

    /**
     * 根据id获取意见
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param id 唯一标识
     * @return OpinionModel
     */
    OpinionModel getById(String tenantId, String userId, String id);

    /**
     * 根据任务id获取意见
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param taskId 任务id
     * @return OpinionModel
     */
    OpinionModel getByTaskId(String tenantId, String userId, String taskId);

    /**
     * 获取意见框历史记录
     *
     * @param tenantId 租户id
     * @param processSerialNumber 流程编号
     * @param opinionFrameMark 意见框Id
     * @return List&lt;OpinionHistoryModel&gt;
     */
    List<OpinionHistoryModel> opinionHistoryList(String tenantId, String processSerialNumber, String opinionFrameMark);

    /**
     * 获取个人意见列表
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param processSerialNumber 流程编号
     * @param taskId 任务id
     * @param itembox 办件状态，todo（待办）,doing（在办）,done（办结）
     * @param opinionFrameMark opinionFrameMark
     * @param itemId 事项id
     * @param taskDefinitionKey 任务定义key
     * @param activitiUser activitiUser
     * @return List&lt;Map&lt;String, Object&gt;&gt;
     */
    List<Map<String, Object>> personCommentList(String tenantId, String userId, String processSerialNumber,
        String taskId, String itembox, String opinionFrameMark, String itemId, String taskDefinitionKey,
        String activitiUser);

    /**
     * 保存意见
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param opinion OpinionModel
     * @throws Exception Exception
     */
    void save(String tenantId, String userId, OpinionModel opinion) throws Exception;

    /**
     *
     * Description: 保存或更新意见
     *
     * @param tenantId 租户id
     * @param userId 人员id
     * @param opinion 意见实体
     * @return OpinionModel
     * @throws Exception Exception
     */
    OpinionModel saveOrUpdate(String tenantId, String userId, OpinionModel opinion) throws Exception;
}