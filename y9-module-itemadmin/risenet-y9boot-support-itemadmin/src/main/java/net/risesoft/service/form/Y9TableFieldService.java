package net.risesoft.service.form;

import java.util.List;

import net.risesoft.entity.form.Y9TableField;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/20
 */
public interface Y9TableFieldService {

    /**
     * 根据id删除表字段定义
     *
     * @param id
     * @return
     */
    void delete(String id);

    /**
     * 根据id获取表字段定义
     *
     * @param id
     * @return
     */
    Y9TableField findById(String id);

    /**
     * 根据表id获取表字段定义
     *
     * @param tableId
     * @return
     */
    List<Y9TableField> listByTableId(String tableId);

    /**
     * 取出当前表定义字段
     *
     * @param tableId
     * @param state 字段状态 ：-1未生成表字段，1为已经生成表字段
     * @return
     */
    List<Y9TableField> listByTableIdAndState(String tableId, Integer state);

    /**
     * 取出当前表定义字段
     *
     * @param tableId
     * @return
     */
    List<Y9TableField> listByTableIdOrderByDisplay(String tableId);

    /**
     * 获取字段系统主键
     *
     * @param tableId
     * @return
     */
    List<Y9TableField> listSystemFieldByTableId(String tableId);

    /**
     * 保存表字段信息
     *
     * @param field
     * @return
     */
    Y9TableField saveOrUpdate(Y9TableField field);

    /**
     * 修改字段状态
     *
     * @param tableId
     */
    void updateState(String tableId);

}
