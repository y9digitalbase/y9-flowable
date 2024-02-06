package net.risesoft.api.itemadmin;

import net.risesoft.model.itemadmin.ActRuDetailModel;
import net.risesoft.model.itemadmin.ItemPage;

public interface ItemTodoApi {

    /**
     * 查询待办任务数量
     *
     * @param tenantId 租户id
     * @param userId 用户id
     * @param systemName 系统名称
     * @return int
     * @throws Exception Exception
     */
    int countByUserIdAndSystemName(String tenantId, String userId, String systemName) throws Exception;

    /**
     * 查询待办任务，以发送时间排序
     *
     * @param tenantId 租户id
     * @param userId 用户id
     * @param systemName 系统名称
     * @param page page
     * @param rows rows
     * @return ItemPage&lt;ActRuDetailModel&gt;
     * @throws Exception Exception
     */
    ItemPage<ActRuDetailModel> findByUserIdAndSystemName(String tenantId, String userId, String systemName,
        Integer page, Integer rows) throws Exception;

    /**
     * 查询待办任务，以发送时间排序
     *
     * @param tenantId 租户id
     * @param userId 用户id
     * @param systemName 系统名称
     * @param tableName 表名称
     * @param searchMapStr 搜索集合
     * @param page page
     * @param rows rows
     * @return ItemPage&lt;ActRuDetailModel&gt;
     * @throws Exception Exception
     */
    ItemPage<ActRuDetailModel> searchByUserIdAndSystemName(String tenantId, String userId, String systemName,
        String tableName, String searchMapStr, Integer page, Integer rows) throws Exception;

}