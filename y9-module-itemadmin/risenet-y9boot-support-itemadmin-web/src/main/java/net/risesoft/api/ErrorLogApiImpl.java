package net.risesoft.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.risesoft.api.itemadmin.ErrorLogApi;
import net.risesoft.entity.ErrorLog;
import net.risesoft.model.itemadmin.ErrorLogModel;
import net.risesoft.service.ErrorLogService;
import net.risesoft.y9.Y9LoginUserHolder;
import net.risesoft.y9.util.Y9BeanUtil;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/22
 */
@RestController
@RequestMapping(value = "/services/rest/errorLog")
public class ErrorLogApiImpl implements ErrorLogApi {

    @Autowired
    private ErrorLogService errorLogService;

    @Override
    @PostMapping(value = "/saveErrorLog", produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    public void saveErrorLog(String tenantId, @RequestBody ErrorLogModel errorLogModel) {
        Y9LoginUserHolder.setTenantId(tenantId);
        ErrorLog errorLog = new ErrorLog();
        Y9BeanUtil.copyProperties(errorLogModel, errorLog);
        errorLogService.saveErrorLog(errorLog);
    }

}