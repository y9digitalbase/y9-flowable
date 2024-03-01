package net.risesoft.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import net.risesoft.consts.UtilConsts;
import net.risesoft.entity.ItemPrintTemplateBind;
import net.risesoft.entity.PrintTemplate;
import net.risesoft.id.IdType;
import net.risesoft.id.Y9IdGenerator;
import net.risesoft.model.user.UserInfo;
import net.risesoft.repository.jpa.PrintTemplateItemBindRepository;
import net.risesoft.repository.jpa.PrintTemplateRepository;
import net.risesoft.service.PrintTemplateService;
import net.risesoft.y9.Y9Context;
import net.risesoft.y9.Y9LoginUserHolder;
import net.risesoft.y9public.entity.Y9FileStore;
import net.risesoft.y9public.service.Y9FileStoreService;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/22
 */
@Transactional(value = "rsTenantTransactionManager", readOnly = true)
@Service(value = "printTemplateService")
public class PrintTemplateServiceImpl implements PrintTemplateService {

    @Autowired
    private Y9FileStoreService y9FileStoreService;

    @Autowired
    private PrintTemplateRepository printTemplateRepository;

    @Autowired
    private PrintTemplateItemBindRepository printTemplateItemBindRepository;

    @Override
    @Transactional(readOnly = false)
    public Map<String, Object> deleteBindPrintTemplate(String id) {
        Map<String, Object> map = new HashMap<String, Object>(16);
        try {
            map.put(UtilConsts.SUCCESS, true);
            map.put("msg", "删除成功");
            ItemPrintTemplateBind bindTemplate = printTemplateItemBindRepository.findById(id).orElse(null);
            if (bindTemplate != null && bindTemplate.getId() != null) {
                printTemplateItemBindRepository.deleteById(bindTemplate.getId());
            }
        } catch (Exception e) {
            map.put(UtilConsts.SUCCESS, false);
            map.put("msg", "删除失败");
            e.printStackTrace();
        }
        return map;
    }

    @Override
    @Transactional(readOnly = false)
    public Map<String, Object> deletePrintTemplate(String id) {
        Map<String, Object> map = new HashMap<String, Object>(16);
        try {
            map.put(UtilConsts.SUCCESS, true);
            map.put("msg", "删除成功");
            PrintTemplate printTemplate = printTemplateRepository.findById(id).orElse(null);
            if (printTemplate != null && printTemplate.getId() != null) {
                printTemplateRepository.deleteById(printTemplate.getId());
                try {
                    y9FileStoreService.deleteFile(printTemplate.getFilePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            map.put(UtilConsts.SUCCESS, false);
            map.put("msg", "删除失败");
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public void download(String id, HttpServletResponse response, HttpServletRequest request) {
        try {
            PrintTemplate printTemplate = printTemplateRepository.findById(id).orElse(null);
            byte[] b = y9FileStoreService.downloadFileToBytes(printTemplate.getFilePath());
            int length = b.length;
            String filename = "", userAgent = "User-Agent", firefox = "firefox", msie = "MSIE";
            if (request.getHeader(userAgent).toLowerCase().indexOf(firefox) > 0) {
                filename = new String(printTemplate.getFileName().getBytes("UTF-8"), "ISO8859-1");
            } else if (request.getHeader(userAgent).toUpperCase().indexOf(msie) > 0) {
                filename = URLEncoder.encode(printTemplate.getFileName(), "UTF-8");
            } else {
                filename = URLEncoder.encode(printTemplate.getFileName(), "UTF-8");
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
            response.setHeader("Content-Length", String.valueOf(length));
            IOUtils.write(b, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PrintTemplate> findAll() {
        return printTemplateRepository.findAllOrderByUploadTimeDesc();
    }

    @Override
    public List<ItemPrintTemplateBind> getTemplateBindList(String itemId) {
        List<ItemPrintTemplateBind> list = new ArrayList<ItemPrintTemplateBind>();
        try {
            ItemPrintTemplateBind itemPrintTemplateBind = printTemplateItemBindRepository.findByItemId(itemId);
            if (itemPrintTemplateBind != null) {
                list.add(itemPrintTemplateBind);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Transactional(readOnly = false)
    @Override
    public Map<String, Object> saveBindTemplate(String itemId, String templateId, String templateName,
        String templateUrl, String templateType) {
        Map<String, Object> map = new HashMap<String, Object>(16);
        try {
            ItemPrintTemplateBind printTemplateItemBind = printTemplateItemBindRepository.findByItemId(itemId);
            if (printTemplateItemBind == null) {
                printTemplateItemBind = new ItemPrintTemplateBind();
                printTemplateItemBind.setId(Y9IdGenerator.genId(IdType.SNOWFLAKE));
                printTemplateItemBind.setTenantId(Y9LoginUserHolder.getTenantId());
                printTemplateItemBind.setItemId(itemId);
                printTemplateItemBind.setTemplateId(templateId);
                printTemplateItemBind.setTemplateName(templateName);
                printTemplateItemBind.setTemplateUrl(templateUrl);
                printTemplateItemBind.setTemplateType(templateType);
                printTemplateItemBindRepository.save(printTemplateItemBind);
            } else {
                printTemplateItemBind.setItemId(itemId);
                printTemplateItemBind.setTemplateId(templateId);
                printTemplateItemBind.setTemplateName(templateName);
                printTemplateItemBind.setTemplateUrl(templateUrl);
                printTemplateItemBind.setTemplateType(templateType);
                printTemplateItemBindRepository.save(printTemplateItemBind);
            }
            map.put(UtilConsts.SUCCESS, true);
            map.put("msg", "保存成功");
        } catch (Exception e) {
            map.put(UtilConsts.SUCCESS, false);
            map.put("msg", "保存失败");
            e.printStackTrace();
        }
        return map;
    }

    @Override
    @Transactional(readOnly = false)
    public void saveOrUpdate(PrintTemplate printTemplate) {
        UserInfo userInfo = Y9LoginUserHolder.getUserInfo();
        String personId = userInfo.getPersonId(), personName = userInfo.getName(),
            tenantId = Y9LoginUserHolder.getTenantId();
        String id = printTemplate.getId();
        if (StringUtils.isNotEmpty(id)) {
            PrintTemplate oldPrint = printTemplateRepository.findById(id).orElse(null);
            if (null != oldPrint) {
                oldPrint.setDescribes(printTemplate.getDescribes());
                oldPrint.setFileName(printTemplate.getFileName());
                oldPrint.setFilePath(printTemplate.getFilePath());
                oldPrint.setFileSize(printTemplate.getFileSize());
                oldPrint.setPersonId(personId);
                oldPrint.setPersonName(personName);
                oldPrint.setTenantId(tenantId);
                oldPrint.setUploadTime(printTemplate.getUploadTime());
                printTemplateRepository.save(oldPrint);
                return;
            } else {
                printTemplateRepository.save(printTemplate);
                return;
            }
        }
        PrintTemplate newPrint = new PrintTemplate();
        newPrint.setId(Y9IdGenerator.genId(IdType.SNOWFLAKE));
        newPrint.setDescribes(printTemplate.getDescribes());
        newPrint.setFileName(printTemplate.getFileName());
        newPrint.setFilePath(printTemplate.getFilePath());
        newPrint.setFileSize(printTemplate.getFileSize());
        newPrint.setPersonId(personId);
        newPrint.setPersonName(personName);
        newPrint.setTenantId(tenantId);
        newPrint.setUploadTime(printTemplate.getUploadTime());
        printTemplateRepository.save(newPrint);
    }

    @Override
    @Transactional(readOnly = false)
    public Map<String, Object> uploadTemplate(MultipartFile file) {
        Map<String, Object> map = new HashMap<String, Object>(16);
        String[] fileNames = file.getOriginalFilename().split("\\\\");
        String fileName = "";
        UserInfo userInfo = Y9LoginUserHolder.getUserInfo();
        try {
            if (file != null) {
                PrintTemplate printTemplate = new PrintTemplate();
                printTemplate.setId(Y9IdGenerator.genId(IdType.SNOWFLAKE));
                if (fileNames.length > 1) {
                    fileName = fileNames[fileNames.length - 1];
                } else {
                    fileName = file.getOriginalFilename();
                }
                printTemplate.setFileName(fileName);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String fullPath = "/" + Y9Context.getSystemName() + "/printTemplate/" + sdf.format(new Date());
                Y9FileStore y9FileStore = y9FileStoreService.uploadFile(file, fullPath, fileName);
                printTemplate.setPersonId(userInfo.getPersonId());
                printTemplate.setPersonName(userInfo.getName());
                printTemplate.setTenantId(Y9LoginUserHolder.getTenantId());
                printTemplate.setUploadTime(new Date());
                printTemplate.setDescribes("");
                printTemplate.setFilePath(y9FileStore.getId());
                printTemplate.setFileSize(y9FileStore.getDisplayFileSize());
                printTemplateRepository.save(printTemplate);
                map.put(UtilConsts.SUCCESS, true);
                map.put("msg", "上传成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put(UtilConsts.SUCCESS, false);
            map.put("msg", "上传失败");
        }
        return map;
    }
}