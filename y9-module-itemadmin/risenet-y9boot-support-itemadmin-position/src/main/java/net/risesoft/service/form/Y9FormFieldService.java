package net.risesoft.service.form;

import java.util.List;
import java.util.Map;

import net.risesoft.entity.form.Y9FormField;
import org.springframework.data.domain.Page;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/20
 */
public interface Y9FormFieldService {

    /**
     * 根据表单id获取表单元素绑定信息
     *
     * @param formId
     * @return
     */
    public List<Y9FormField> findByFormId(String formId);

    /**
     * Description:
     *
     * @param id
     * @return
     */
    public Y9FormField findById(String id);

    /**
     * 根据tableName查找绑定信息
     *
     * @param tableName
     * @return
     */
    public List<Y9FormField> findByTableName(String tableName);

    /**
     * 根据表名和表单id获取绑定字段
     *
     * @param tableName
     * @param formId
     * @return
     */
    public List<Y9FormField> findByTableNameAndFormId(String tableName, String formId);

    /**
     * Description:
     *
     * @param formField
     * @return
     */
    public Map<String, Object> saveOrUpdate(Y9FormField formField);

    /**
     * 获取表单绑定的业务表字段
     * @param formId
     * @param page
     * @param rows
     * @return
     */
    public Page<Y9FormField> findByFormId(String formId, Integer page, Integer rows);

    /**
     * 删除表单绑定字段
     * @param id
     * @return
     */
    Map<String,Object> deleteFormFieldBind(String id);

}