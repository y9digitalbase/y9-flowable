package net.risesoft.service;

import java.util.List;

import org.springframework.data.domain.Page;

import net.risesoft.entity.ViewType;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/22
 */
public interface ViewTypeService {

    /**
     * Description:
     * 
     * @return
     */
    List<ViewType> findAll();

    /**
     * Description:
     * 
     * @param page
     * @param rows
     * @return
     */
    Page<ViewType> findAll(int page, int rows);

    /**
     * Description:
     * 
     * @param id
     * @return
     */
    ViewType findById(String id);

    /**
     * Description:
     * 
     * @param mark
     * @return
     */
    ViewType findByMark(String mark);

    /**
     * Description:
     * 
     * @param id
     */
    void remove(String id);

    /**
     * Description:
     * 
     * @param ids
     */
    void remove(String[] ids);

    /**
     * Description:
     * 
     * @param viewType
     * @return
     */
    ViewType save(ViewType viewType);

    /**
     * Description:
     * 
     * @param viewType
     * @return
     */
    ViewType saveOrUpdate(ViewType viewType);

    /**
     * Description:
     * 
     * @param page
     * @param rows
     * @param keyword
     * @return
     */
    Page<ViewType> search(int page, int rows, String keyword);
}