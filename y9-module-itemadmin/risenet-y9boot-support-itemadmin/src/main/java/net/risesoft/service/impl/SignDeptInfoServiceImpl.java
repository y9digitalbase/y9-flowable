package net.risesoft.service.impl;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.risesoft.api.platform.org.DepartmentApi;
import net.risesoft.entity.SignDeptInfo;
import net.risesoft.model.platform.Department;
import net.risesoft.repository.jpa.SignDeptInfoRepository;
import net.risesoft.service.SignDeptInfoService;
import net.risesoft.y9.Y9LoginUserHolder;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(value = "rsTenantTransactionManager", readOnly = true)
public class SignDeptInfoServiceImpl implements SignDeptInfoService {

    private final SignDeptInfoRepository signDeptInfoRepository;

    private final DepartmentApi departmentApi;

    @Override
    @Transactional
    public void deleteById(String id) {
        signDeptInfoRepository.deleteById(id);
    }

    @Override
    public List<SignDeptInfo> getSignDeptList(String processInstanceId, String deptType) {
        return signDeptInfoRepository.findByProcessInstanceIdAndDeptTypeOrderByOrderIndexAsc(processInstanceId,
            deptType);
    }

    @Override
    @Transactional
    public void saveSignDept(String processInstanceId, String deptType, String deptIds) {
        String[] split = deptIds.split(",");
        List<String> split1 = Arrays.asList(split);
        signDeptInfoRepository.deleteByProcessInstanceIdAndDeptTypeAndDeptIdNotIn(processInstanceId, deptType, split1);
        for (int i = 0; i < split.length; i++) {
            String deptId = split[i];
            SignDeptInfo signDeptInfo =
                signDeptInfoRepository.findByProcessInstanceIdAndDeptTypeAndDeptId(processInstanceId, deptType, deptId);
            if (signDeptInfo != null) {
                signDeptInfo.setOrderIndex(i + 1);
            } else {
                signDeptInfo = new SignDeptInfo();
                signDeptInfo.setInputPerson(Y9LoginUserHolder.getOrgUnit().getName());
                signDeptInfo.setInputPersonId(Y9LoginUserHolder.getOrgUnitId());
                signDeptInfo.setOrderIndex(i + 1);
                signDeptInfo.setDeptId(deptId);
                Department department = departmentApi.get(Y9LoginUserHolder.getTenantId(), deptId).getData();
                signDeptInfo.setDeptName(department != null ? department.getName() : "部门不存在");
                signDeptInfo.setProcessInstanceId(processInstanceId);
                signDeptInfo.setDeptType(deptType);
            }
            signDeptInfoRepository.save(signDeptInfo);
        }
    }

    @Override
    @Transactional
    public void saveSignDeptInfo(String id, String userName) {
        SignDeptInfo signDeptInfo = signDeptInfoRepository.findById(id).orElse(null);
        if (signDeptInfo != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            signDeptInfo.setUserName(userName);
            signDeptInfo.setSignDate(simpleDateFormat.format(new Date()));
            signDeptInfoRepository.save(signDeptInfo);
        }
    }
}