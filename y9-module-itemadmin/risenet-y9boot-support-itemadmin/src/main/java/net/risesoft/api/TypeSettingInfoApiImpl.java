package net.risesoft.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import net.risesoft.api.itemadmin.TypeSettingInfoApi;
import net.risesoft.entity.TypeSettingInfo;
import net.risesoft.model.itemadmin.TypeSettingInfoModel;
import net.risesoft.pojo.Y9Result;
import net.risesoft.service.TypeSettingInfoService;
import net.risesoft.y9.Y9LoginUserHolder;
import net.risesoft.y9.util.Y9BeanUtil;

/**
 * 发文单排版信息接口
 *
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/20
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/services/rest/typeSetting", produces = MediaType.APPLICATION_JSON_VALUE)
public class TypeSettingInfoApiImpl implements TypeSettingInfoApi {

    private final TypeSettingInfoService typeSettingInfoService;

    /**
     * 删除排版信息
     *
     * @param tenantId 租户id
     * @param id 排版信息id
     * @return Y9Result<Object>
     */
    @Override
    public Y9Result<Object> delTypeSetting(@RequestParam String tenantId, @RequestParam String id) {
        Y9LoginUserHolder.setTenantId(tenantId);
        typeSettingInfoService.delTypeSetting(id);
        return Y9Result.success();
    }

    /**
     * 获取排版信息
     *
     * @param tenantId 租户id
     * @param processSerialNumber 流程编号
     * @return 排版信息
     */
    @Override
    public Y9Result<List<TypeSettingInfoModel>> getList(@RequestParam String tenantId,
        @RequestParam String processSerialNumber) {
        Y9LoginUserHolder.setTenantId(tenantId);
        List<TypeSettingInfo> list = typeSettingInfoService.findByProcessSerialNumber(processSerialNumber);
        List<TypeSettingInfoModel> modelList = new ArrayList<>();
        for (TypeSettingInfo typeSettingInfo : list) {
            TypeSettingInfoModel model = new TypeSettingInfoModel();
            Y9BeanUtil.copyProperties(typeSettingInfo, model);
            modelList.add(model);
        }
        return Y9Result.success(modelList);
    }

    /**
     * 保存排版信息
     *
     * @param tenantId 租户id
     * @param processSerialNumber 流程编号
     * @param jsonData 排版信息
     * @return 排版信息
     */
    @Override
    public Y9Result<Object> saveTypeSetting(@RequestParam String tenantId, @RequestParam String processSerialNumber,
        @RequestParam String jsonData) {
        Y9LoginUserHolder.setTenantId(tenantId);
        typeSettingInfoService.saveTypeSetting(processSerialNumber, jsonData);
        return Y9Result.success();
    }
}