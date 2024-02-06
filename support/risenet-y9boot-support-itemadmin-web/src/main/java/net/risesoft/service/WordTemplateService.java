package net.risesoft.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import net.risesoft.entity.WordTemplate;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/22
 */
public interface WordTemplateService {

    /**
     * 删除正文模板
     * 
     * @param id
     * @return
     */
    Map<String, Object> deleteWordTemplate(String id);

    /**
     * 下载模板
     * 
     * @param id
     * @param response
     * @param request
     */
    void download(String id, HttpServletResponse response, HttpServletRequest request);

    /**
     * Description:
     * 
     * @return
     */
    List<WordTemplate> findAll();

    /**
     * Description:
     * 
     * @param bureauId
     * @return
     */
    List<WordTemplate> findByBureauIdOrderByUploadTimeDesc(String bureauId);

    /**
     * Description:
     * 
     * @param id
     * @return
     */
    WordTemplate findById(String id);

    /**
     * Description: 获取书签
     * 
     * @param wordTemplateId
     * @param wordTemplateType
     * @return
     */
    Map<String, Object> getBookMarkList(String wordTemplateId, String wordTemplateType);

    /**
     * Description: 保存正文模板
     * 
     * @param wordTemplate
     */
    void saveOrUpdate(WordTemplate wordTemplate);

    /**
     * 上传正文模板
     * 
     * @param file
     * @return
     */
    Map<String, Object> upload(MultipartFile file);
}