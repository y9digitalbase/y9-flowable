package net.risesoft.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.risesoft.api.itemadmin.FormDataApi;
import net.risesoft.api.itemadmin.ItemAllApi;
import net.risesoft.api.itemadmin.ItemApi;
import net.risesoft.api.itemadmin.ItemDoingApi;
import net.risesoft.api.itemadmin.ItemDoneApi;
import net.risesoft.api.itemadmin.ItemHaveDoneApi;
import net.risesoft.api.itemadmin.ItemRecycleApi;
import net.risesoft.api.itemadmin.ItemTodoApi;
import net.risesoft.api.itemadmin.ProcessParamApi;
import net.risesoft.api.itemadmin.SignDeptDetailApi;
import net.risesoft.api.itemadmin.TaskRelatedApi;
import net.risesoft.api.itemadmin.UrgeInfoApi;
import net.risesoft.api.platform.org.OrgUnitApi;
import net.risesoft.api.processadmin.HistoricTaskApi;
import net.risesoft.api.processadmin.IdentityApi;
import net.risesoft.api.processadmin.ProcessDefinitionApi;
import net.risesoft.api.processadmin.TaskApi;
import net.risesoft.api.processadmin.VariableApi;
import net.risesoft.enums.ActRuDetailStatusEnum;
import net.risesoft.enums.ItemBoxTypeEnum;
import net.risesoft.enums.TaskRelatedEnum;
import net.risesoft.model.itemadmin.ActRuDetailModel;
import net.risesoft.model.itemadmin.ItemModel;
import net.risesoft.model.itemadmin.ProcessParamModel;
import net.risesoft.model.itemadmin.QueryParamModel;
import net.risesoft.model.itemadmin.SignDeptDetailModel;
import net.risesoft.model.itemadmin.TaskRelatedModel;
import net.risesoft.model.itemadmin.UrgeInfoModel;
import net.risesoft.model.platform.OrgUnit;
import net.risesoft.model.platform.Position;
import net.risesoft.model.processadmin.HistoricTaskInstanceModel;
import net.risesoft.model.processadmin.IdentityLinkModel;
import net.risesoft.model.processadmin.TaskModel;
import net.risesoft.pojo.Y9Page;
import net.risesoft.pojo.Y9Result;
import net.risesoft.service.WorkDayService;
import net.risesoft.service.WorkList4GfgService;
import net.risesoft.util.SysVariables;
import net.risesoft.y9.Y9LoginUserHolder;
import net.risesoft.y9.json.Y9JsonUtil;
import net.risesoft.y9.util.Y9Util;

/**
 * @author qinman
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkList4GfgServiceImpl implements WorkList4GfgService {

    private final ItemApi itemApi;

    private final VariableApi variableApi;

    private final HistoricTaskApi historicTaskApi;

    private final ProcessParamApi processParamApi;

    private final ProcessDefinitionApi processDefinitionApi;

    private final FormDataApi formDataApi;

    private final ItemTodoApi itemTodoApi;

    private final ItemDoingApi itemDoingApi;

    private final ItemDoneApi itemDoneApi;

    private final ItemRecycleApi itemRecycleApi;

    private final ItemHaveDoneApi itemHaveDoneApi;

    private final ItemAllApi itemAllApi;

    private final TaskApi taskApi;

    private final OrgUnitApi orgUnitApi;

    private final IdentityApi identityApi;

    private final TaskRelatedApi taskRelatedApi;

    private final WorkDayService workDayService;

    private final SignDeptDetailApi signDeptDetailApi;

    private final UrgeInfoApi urgeInfoApi;

    private final HistoricTaskApi historictaskApi;

    @Override
    public Y9Page<Map<String, Object>> allList(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemAllApi.findByUserIdAndSystemName(tenantId, positionId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put("actRuDetailId", ardModel.getId());
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    if (StringUtils.isBlank(processParam.getCompleter())) {
                        List<TaskModel> taskList =
                            taskApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                        boolean isSubProcessChildNode = processDefinitionApi.isSubProcessChildNode(tenantId,
                            taskList.get(0).getProcessDefinitionId(), taskList.get(0).getTaskDefinitionKey()).getData();
                        if (isSubProcessChildNode) {
                            boolean isSignDept = signDeptDetailApi
                                .findByProcessSerialNumber(tenantId, processSerialNumber).getData().stream().anyMatch(
                                    signDeptDetailModel -> signDeptDetailModel.getDeptId().equals(bureau.getId()));
                            if (!isSignDept) {
                                // 针对SubProcess
                                String mainSender = variableApi.getVariableByProcessInstanceId(tenantId,
                                    processInstanceId, SysVariables.MAINSENDER).getData();
                                mapTemp.put("taskAssignee", StringUtils.isBlank(mainSender) ? "无"
                                    : Y9JsonUtil.readValue(mainSender, String.class));
                                mapTemp.put("taskName", "送会签");
                            } else {
                                List<String> listTemp = getAssigneeIdsAndAssigneeNames4SignDept(taskList, taskId);
                                mapTemp.put("taskName", listTemp.get(0));
                                mapTemp.put("taskAssignee", listTemp.get(1));
                            }
                        } else {
                            List<String> listTemp = getAssigneeIdsAndAssigneeNames(taskList);
                            mapTemp.put("taskName", taskList.get(0).getName());
                            mapTemp.put("taskAssignee", listTemp.get(0));
                        }
                    } else {
                        mapTemp.put("taskName", "已办结");
                        mapTemp.put("taskAssignee", processParam.getCompleter());
                    }
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);

                    if (Objects.equals(ardModel.getStatus(), ActRuDetailStatusEnum.TODO.getValue())) {
                        mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.TODO.getValue());
                    } else {
                        mapTemp.put(SysVariables.ITEMBOX, StringUtils.isBlank(processParam.getCompleter())
                            ? ItemBoxTypeEnum.DOING.getValue() : ItemBoxTypeEnum.DONE.getValue());
                    }
                } catch (Exception e) {
                    LOGGER.error("获取已办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> allTodoList(QueryParamModel queryParamModel) {
        Y9Page<ActRuDetailModel> itemPage;
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            itemPage = itemTodoApi.findByUserId(tenantId, positionId, queryParamModel);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (queryParamModel.getPage() - 1) * queryParamModel.getRows();
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
                    mapTemp.put("actRuDetailId", ardModel.getId());
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("number", processParam.getCustomNumber());
                    mapTemp.put("title", processParam.getTitle());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", ardModel.getTaskDefName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("taskAssignee", ardModel.getAssigneeName());
                    List<TaskRelatedModel> taskRelatedList = taskRelatedApi.findByTaskId(tenantId, taskId).getData();
                    if (ardModel.isStarted()) {
                        taskRelatedList.add(0, new TaskRelatedModel(TaskRelatedEnum.NEWTODO.getValue(), "新"));
                    }
                    /*
                     * 红绿灯
                     */
                    if (null != ardModel.getDueDate()) {
                        taskRelatedList.add(workDayService.getLightColor(new Date(), ardModel.getDueDate()));
                    }
                    taskRelatedList = taskRelatedList.stream().filter(t -> Integer.parseInt(t.getInfoType()) < Integer
                        .parseInt(TaskRelatedEnum.ACTIONNAME.getValue())).collect(Collectors.toList());
                    List<UrgeInfoModel> urgeInfoList =
                        urgeInfoApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
                    if (ardModel.isSub()) {
                        urgeInfoList = urgeInfoList.stream().filter(
                            urgeInfo -> urgeInfo.isSub() && urgeInfo.getExecutionId().equals(ardModel.getExecutionId()))
                            .collect(Collectors.toList());
                    } else {
                        urgeInfoList =
                            urgeInfoList.stream().filter(urgeInfo -> !urgeInfo.isSub()).collect(Collectors.toList());
                    }
                    if (!urgeInfoList.isEmpty()) {
                        taskRelatedList.add(new TaskRelatedModel(TaskRelatedEnum.URGE.getValue(),
                            Y9JsonUtil.writeValueAsString(urgeInfoList)));
                    }
                    mapTemp.put(SysVariables.TASKRELATEDLIST, taskRelatedList);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.TODO.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取待办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(queryParamModel.getPage(), itemPage.getTotalPages(), itemPage.getTotal(), items,
                "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办列表失败", e);
        }
        return Y9Page.success(queryParamModel.getPage(), 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doingList(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemDoingApi.findByUserIdAndSystemName(tenantId, positionId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    List<TaskModel> taskList = taskApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    boolean isSubProcessChildNode = processDefinitionApi.isSubProcessChildNode(tenantId,
                        taskList.get(0).getProcessDefinitionId(), taskList.get(0).getTaskDefinitionKey()).getData();
                    if (isSubProcessChildNode) {
                        boolean isSignDept = signDeptDetailApi.findByProcessSerialNumber(tenantId, processSerialNumber)
                            .getData().stream()
                            .anyMatch(signDeptDetailModel -> signDeptDetailModel.getDeptId().equals(bureau.getId()));
                        if (!isSignDept) {
                            // 针对SubProcess
                            String mainSender = variableApi
                                .getVariableByProcessInstanceId(tenantId, processInstanceId, SysVariables.MAINSENDER)
                                .getData();
                            mapTemp.put("taskAssignee",
                                StringUtils.isBlank(mainSender) ? "无" : Y9JsonUtil.readValue(mainSender, String.class));
                            mapTemp.put("taskName", "送会签");
                        } else {
                            List<String> listTemp = getAssigneeIdsAndAssigneeNames4SignDept(taskList, taskId);
                            mapTemp.put("taskName", listTemp.get(0));
                            mapTemp.put("taskAssignee", listTemp.get(1));
                        }
                    } else {
                        List<String> listTemp = getAssigneeIdsAndAssigneeNames(taskList);
                        mapTemp.put("taskName", taskList.get(0).getName());
                        mapTemp.put("taskAssignee", listTemp.get(0));
                    }
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);

                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DOING.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取在办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doingList4DuBan(String itemId, Integer days, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            String dueDate = workDayService.getDate(new Date(), days);
            System.out.println(dueDate);
            if (StringUtils.isBlank(dueDate)) {
                return Y9Page.failure(0, 0, 0, new ArrayList<>(), "未设置日历", 500);
            }
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemDoingApi.findBySystemName4DuBan(tenantId, dueDate, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    List<TaskModel> taskList = taskApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    boolean isSubProcessChildNode = processDefinitionApi.isSubProcessChildNode(tenantId,
                        taskList.get(0).getProcessDefinitionId(), taskList.get(0).getTaskDefinitionKey()).getData();
                    if (isSubProcessChildNode) {
                        boolean isSignDept = signDeptDetailApi.findByProcessSerialNumber(tenantId, processSerialNumber)
                            .getData().stream()
                            .anyMatch(signDeptDetailModel -> signDeptDetailModel.getDeptId().equals(bureau.getId()));
                        if (!isSignDept) {
                            // 针对SubProcess
                            String mainSender = variableApi
                                .getVariableByProcessInstanceId(tenantId, processInstanceId, SysVariables.MAINSENDER)
                                .getData();
                            mapTemp.put("taskAssignee",
                                StringUtils.isBlank(mainSender) ? "无" : Y9JsonUtil.readValue(mainSender, String.class));
                            mapTemp.put("taskName", "送会签");
                        } else {
                            List<String> listTemp = getAssigneeIdsAndAssigneeNames4SignDept(taskList, taskId);
                            mapTemp.put("taskName", listTemp.get(0));
                            mapTemp.put("taskAssignee", listTemp.get(1));
                        }
                    } else {
                        List<String> listTemp = getAssigneeIdsAndAssigneeNames(taskList);
                        mapTemp.put("taskName", taskList.get(0).getName());
                        mapTemp.put("taskAssignee", listTemp.get(0));
                    }
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);

                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DOING.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取在办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doingList4Dept(String itemId, boolean isBureau, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            Position position = Y9LoginUserHolder.getPosition();
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage;
            if (isBureau) {
                itemPage = itemDoingApi.findByDeptIdAndSystemName(tenantId, bureau.getId(), true, item.getSystemName(),
                    page, rows);
            } else {
                itemPage = itemDoingApi.findByDeptIdAndSystemName(tenantId, position.getParentId(), false,
                    item.getSystemName(), page, rows);
            }
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    List<TaskModel> taskList = taskApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    boolean isSubProcessChildNode = processDefinitionApi.isSubProcessChildNode(tenantId,
                        taskList.get(0).getProcessDefinitionId(), taskList.get(0).getTaskDefinitionKey()).getData();
                    if (isSubProcessChildNode) {
                        boolean isSignDept = signDeptDetailApi.findByProcessSerialNumber(tenantId, processSerialNumber)
                            .getData().stream()
                            .anyMatch(signDeptDetailModel -> signDeptDetailModel.getDeptId().equals(bureau.getId()));
                        if (!isSignDept) {
                            // 针对SubProcess
                            String mainSender = variableApi
                                .getVariableByProcessInstanceId(tenantId, processInstanceId, SysVariables.MAINSENDER)
                                .getData();
                            mapTemp.put("taskAssignee",
                                StringUtils.isBlank(mainSender) ? "无" : Y9JsonUtil.readValue(mainSender, String.class));
                            mapTemp.put("taskName", "送会签");
                        } else {
                            List<String> listTemp = getAssigneeIdsAndAssigneeNames4SignDept(taskList, taskId);
                            mapTemp.put("taskName", listTemp.get(0));
                            mapTemp.put("taskAssignee", listTemp.get(1));
                        }
                    } else {
                        List<String> listTemp = getAssigneeIdsAndAssigneeNames(taskList);
                        mapTemp.put("taskName", taskList.get(0).getName());
                        mapTemp.put("taskAssignee", listTemp.get(0));
                    }
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);

                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DOING.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取在办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doingList4All(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemDoingApi.findBySystemName(tenantId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    List<TaskModel> taskList = taskApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    boolean isSubProcessChildNode = processDefinitionApi.isSubProcessChildNode(tenantId,
                        taskList.get(0).getProcessDefinitionId(), taskList.get(0).getTaskDefinitionKey()).getData();
                    if (isSubProcessChildNode) {
                        boolean isSignDept = signDeptDetailApi.findByProcessSerialNumber(tenantId, processSerialNumber)
                            .getData().stream()
                            .anyMatch(signDeptDetailModel -> signDeptDetailModel.getDeptId().equals(bureau.getId()));
                        if (!isSignDept) {
                            // 针对SubProcess
                            String mainSender = variableApi
                                .getVariableByProcessInstanceId(tenantId, processInstanceId, SysVariables.MAINSENDER)
                                .getData();
                            mapTemp.put("taskAssignee",
                                StringUtils.isBlank(mainSender) ? "无" : Y9JsonUtil.readValue(mainSender, String.class));
                            mapTemp.put("taskName", "送会签");
                        } else {
                            List<String> listTemp = getAssigneeIdsAndAssigneeNames4SignDept(taskList, taskId);
                            mapTemp.put("taskName", listTemp.get(0));
                            mapTemp.put("taskAssignee", listTemp.get(1));
                        }
                    } else {
                        List<String> listTemp = getAssigneeIdsAndAssigneeNames(taskList);
                        mapTemp.put("taskName", taskList.get(0).getName());
                        mapTemp.put("taskAssignee", listTemp.get(0));
                    }
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);

                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DOING.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取在办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doneList(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemDoneApi.findByUserIdAndSystemName(tenantId, positionId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", "已办结");
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("completer",
                        StringUtils.isBlank(processParam.getCompleter()) ? "无" : processParam.getCompleter());
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DONE.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取待办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doneList4Dept(String itemId, boolean isBureau, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            Position position = Y9LoginUserHolder.getPosition();
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage;
            if (isBureau) {
                itemPage = itemDoneApi.findByDeptIdAndSystemName(tenantId, bureau.getId(), true, item.getSystemName(),
                    page, rows);
            } else {
                itemPage = itemDoneApi.findByDeptIdAndSystemName(tenantId, position.getParentId(), false,
                    item.getSystemName(), page, rows);
            }
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", "已办结");
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("completer",
                        StringUtils.isBlank(processParam.getCompleter()) ? "无" : processParam.getCompleter());
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DONE.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取待办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> doneList4All(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemDoneApi.findBySystemName(tenantId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", "已办结");
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("completer",
                        StringUtils.isBlank(processParam.getCompleter()) ? "无" : processParam.getCompleter());
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DONE.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取待办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    /**
     * 当并行的时候，会获取到多个task，为了并行时当前办理人显示多人，而不是显示多条记录，需要分开分别进行处理
     *
     * @return List<String>
     */
    private List<String> getAssigneeIdsAndAssigneeNames(List<TaskModel> taskList) {
        String tenantId = Y9LoginUserHolder.getTenantId();
        String assigneeNames = "";
        List<String> list = new ArrayList<>();
        int i = 0;
        for (TaskModel task : taskList) {
            if (StringUtils.isEmpty(assigneeNames)) {
                String assignee = task.getAssignee();
                if (StringUtils.isNotBlank(assignee)) {
                    OrgUnit personTemp = orgUnitApi.getOrgUnitPersonOrPosition(tenantId, assignee).getData();
                    if (personTemp != null) {
                        assigneeNames = personTemp.getName();
                        i += 1;
                    }
                } else {// 处理单实例未签收的当前办理人显示
                    List<IdentityLinkModel> iList =
                        identityApi.getIdentityLinksForTask(tenantId, task.getId()).getData();
                    if (!iList.isEmpty()) {
                        int j = 0;
                        for (IdentityLinkModel identityLink : iList) {
                            String assigneeId = identityLink.getUserId();
                            OrgUnit ownerUser = orgUnitApi
                                .getOrgUnitPersonOrPosition(Y9LoginUserHolder.getTenantId(), assigneeId).getData();
                            if (j < 5) {
                                assigneeNames = Y9Util.genCustomStr(assigneeNames, ownerUser.getName(), "、");
                            } else {
                                assigneeNames = assigneeNames + "等，共" + iList.size() + "人";
                                break;
                            }
                            j++;
                        }
                    }
                }
            } else {
                String assignee = task.getAssignee();
                if (i < 5) {
                    if (StringUtils.isNotBlank(assignee)) {
                        OrgUnit personTemp = orgUnitApi.getOrgUnitPersonOrPosition(tenantId, assignee).getData();
                        if (personTemp != null) {
                            // 并行时，领导选取时存在顺序，因此这里也存在顺序
                            assigneeNames = Y9Util.genCustomStr(assigneeNames, personTemp.getName(), "、");
                            i += 1;
                        }
                    }
                }
            }
        }
        if (taskList.size() > 5) {
            assigneeNames += "等，共" + taskList.size() + "人";
        }
        list.add(assigneeNames);
        return list;
    }

    /**
     * 返回当前人参与过的子流程的任务
     *
     * @param taskList 当前子流程所有任务
     * @param taskId 当前人参与过的任务
     * @return List<String>
     */
    private List<String> getAssigneeIdsAndAssigneeNames4SignDept(List<TaskModel> taskList, String taskId) {
        String tenantId = Y9LoginUserHolder.getTenantId();
        String taskName = "", assigneeNames = "";
        List<String> list = new ArrayList<>();
        int i = 0;
        HistoricTaskInstanceModel hisTask = historicTaskApi.getById(tenantId, taskId).getData();
        for (TaskModel task : taskList) {
            if (!task.getExecutionId().equals(hisTask.getExecutionId())) {
                continue;
            }
            taskName = task.getName();
            if (StringUtils.isEmpty(assigneeNames)) {
                String assignee = task.getAssignee();
                if (StringUtils.isNotBlank(assignee)) {
                    OrgUnit personTemp = orgUnitApi.getOrgUnitPersonOrPosition(tenantId, assignee).getData();
                    if (personTemp != null) {
                        assigneeNames = personTemp.getName();
                        i += 1;
                    }
                } else {// 处理单实例未签收的当前办理人显示
                    List<IdentityLinkModel> iList =
                        identityApi.getIdentityLinksForTask(tenantId, task.getId()).getData();
                    if (!iList.isEmpty()) {
                        int j = 0;
                        for (IdentityLinkModel identityLink : iList) {
                            String assigneeId = identityLink.getUserId();
                            OrgUnit ownerUser = orgUnitApi
                                .getOrgUnitPersonOrPosition(Y9LoginUserHolder.getTenantId(), assigneeId).getData();
                            if (j < 5) {
                                assigneeNames = Y9Util.genCustomStr(assigneeNames, ownerUser.getName(), "、");
                            } else {
                                assigneeNames = assigneeNames + "等，共" + iList.size() + "人";
                                break;
                            }
                            j++;
                        }
                    }
                }
            } else {
                String assignee = task.getAssignee();
                if (i < 5) {
                    if (StringUtils.isNotBlank(assignee)) {
                        OrgUnit personTemp = orgUnitApi.getOrgUnitPersonOrPosition(tenantId, assignee).getData();
                        if (personTemp != null) {
                            // 并行时，领导选取时存在顺序，因此这里也存在顺序
                            assigneeNames = Y9Util.genCustomStr(assigneeNames, personTemp.getName(), "、");
                            i += 1;
                        }
                    }
                }
            }
        }
        if (taskList.size() > 5) {
            assigneeNames += "等，共" + taskList.size() + "人";
        }

        list.add(taskName);
        list.add(assigneeNames);
        return list;
    }

    /**
     * 返回会签流程的当前办理人和当前办理环节
     *
     * @param taskList 当前流程正在运行的所有任务
     * @param executionId 会签流程的执行id
     * @return List<String>
     */
    private List<String> getTaskNameAndAssigneeNames(List<TaskModel> taskList, String executionId) {
        String tenantId = Y9LoginUserHolder.getTenantId();
        String taskName = "", assigneeNames = "";
        List<String> list = new ArrayList<>();
        int i = 0;
        for (TaskModel task : taskList) {
            if (!task.getExecutionId().equals(executionId)) {
                continue;
            }
            taskName = task.getName();
            if (StringUtils.isEmpty(assigneeNames)) {
                String assignee = task.getAssignee();
                if (StringUtils.isNotBlank(assignee)) {
                    OrgUnit personTemp = orgUnitApi.getOrgUnitPersonOrPosition(tenantId, assignee).getData();
                    if (personTemp != null) {
                        assigneeNames = personTemp.getName();
                        i += 1;
                    }
                } else {// 处理单实例未签收的当前办理人显示
                    List<IdentityLinkModel> iList =
                        identityApi.getIdentityLinksForTask(tenantId, task.getId()).getData();
                    if (!iList.isEmpty()) {
                        int j = 0;
                        for (IdentityLinkModel identityLink : iList) {
                            String assigneeId = identityLink.getUserId();
                            OrgUnit ownerUser = orgUnitApi
                                .getOrgUnitPersonOrPosition(Y9LoginUserHolder.getTenantId(), assigneeId).getData();
                            if (j < 5) {
                                assigneeNames = Y9Util.genCustomStr(assigneeNames, ownerUser.getName(), "、");
                            } else {
                                assigneeNames = assigneeNames + "等，共" + iList.size() + "人";
                                break;
                            }
                            j++;
                        }
                    }
                }
            } else {
                String assignee = task.getAssignee();
                if (i < 5) {
                    if (StringUtils.isNotBlank(assignee)) {
                        OrgUnit personTemp = orgUnitApi.getOrgUnitPersonOrPosition(tenantId, assignee).getData();
                        if (personTemp != null) {
                            assigneeNames = Y9Util.genCustomStr(assigneeNames, personTemp.getName(), "、");
                            i += 1;
                        }
                    }
                }
            }
        }
        if (taskList.size() > 5) {
            assigneeNames += "等，共" + taskList.size() + "人";
        }
        list.add(StringUtils.isBlank(taskName) ? "已办结" : taskName);
        list.add(StringUtils.isBlank(assigneeNames) ? "无" : assigneeNames);
        return list;
    }

    @Override
    public Y9Page<Map<String, Object>> haveDoneList(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemHaveDoneApi.findByUserIdAndSystemName(tenantId, positionId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("id", processSerialNumber);
                    mapTemp.put("serialNumber", ++serialNumber);
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, StringUtils.isBlank(processParam.getCompleter())
                        ? ItemBoxTypeEnum.DOING.getValue() : ItemBoxTypeEnum.DONE.getValue());
                    HistoricTaskInstanceModel hisTask;
                    if (ardModel.isEnded()) {
                        LocalDate createTime =
                            ardModel.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        hisTask = historictaskApi
                            .getById(tenantId, ardModel.getTaskId(), String.valueOf(createTime.getYear())).getData();
                    } else {
                        hisTask = historictaskApi.getById(tenantId, ardModel.getTaskId()).getData();
                    }
                    mapTemp.put("processDefinitionId", hisTask.getProcessDefinitionId());
                    boolean isSub = processDefinitionApi.isSubProcessChildNode(tenantId,
                        hisTask.getProcessDefinitionId(), hisTask.getTaskDefinitionKey()).getData();
                    List<TaskModel> taskList = new ArrayList<>();
                    List<SignDeptDetailModel> signDeptDetailModels = new ArrayList<>();
                    if (!ardModel.isEnded()) {
                        taskList = taskApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                        boolean isSubProcessChildNode = processDefinitionApi.isSubProcessChildNode(tenantId,
                            taskList.get(0).getProcessDefinitionId(), taskList.get(0).getTaskDefinitionKey()).getData();
                        if (isSubProcessChildNode) {
                            if (!isSub) {
                                String mainSender = variableApi.getVariableByProcessInstanceId(tenantId,
                                    processInstanceId, SysVariables.MAINSENDER).getData();
                                signDeptDetailModels = signDeptDetailApi
                                    .findByProcessSerialNumber(tenantId, processSerialNumber).getData();
                                mapTemp.put("taskName", historictaskApi
                                    .getById(tenantId, signDeptDetailModels.get(0).getTaskId()).getData().getName());
                                mapTemp.put("taskAssignee", StringUtils.isBlank(mainSender) ? "无"
                                    : Y9JsonUtil.readValue(mainSender, String.class));
                            } else {
                                List<String> listTemp = getAssigneeIdsAndAssigneeNames4SignDept(taskList, taskId);
                                mapTemp.put("taskName", listTemp.get(0));
                                mapTemp.put("taskAssignee", listTemp.get(1));
                            }
                        } else {
                            List<String> listTemp = getAssigneeIdsAndAssigneeNames(taskList);
                            mapTemp.put("taskName", taskList.get(0).getName());
                            mapTemp.put("taskAssignee", listTemp.get(0));
                        }
                    } else {
                        mapTemp.put("taskName", "已办结");
                        mapTemp.put("taskAssignee", processParam.getCompleter());
                    }
                    List<Map<String, Object>> childrenList = new ArrayList<>();
                    if (!isSub) {
                        List<TaskModel> finalTaskList = taskList;
                        Map<String, Object> finalMapTemp = mapTemp;
                        AtomicInteger count = new AtomicInteger(0);
                        if (signDeptDetailModels.isEmpty()) {
                            signDeptDetailModels =
                                signDeptDetailApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
                        }
                        signDeptDetailModels.forEach(sdd -> {
                            List<String> taskNameAndAssigneeNames =
                                getTaskNameAndAssigneeNames(finalTaskList, sdd.getExecutionId());
                            Map<String, Object> childrenMap = new HashMap<>(finalMapTemp);
                            childrenMap.put("id", sdd.getId());
                            childrenMap.put("serialNumber", count.incrementAndGet());
                            childrenMap.put("taskName", taskNameAndAssigneeNames.get(0));
                            childrenMap.put("taskAssignee", taskNameAndAssigneeNames.get(1));
                            childrenMap.put("children", List.of());
                            childrenMap.put("status", sdd.getStatus());
                            childrenMap.put("bureauName", sdd.getDeptName());
                            childrenList.add(childrenMap);
                        });
                    }
                    mapTemp.put("children", childrenList);
                } catch (Exception e) {
                    LOGGER.error("获取已办列表失败" + processInstanceId, e);
                }
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Result<List<Map<String, Object>>> getSignDeptDetailList(String processSerialNumber) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId();
            ProcessParamModel processParam =
                processParamApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
            Map<String, Object> mapTemp = new HashMap<>();
            mapTemp.put("id", processSerialNumber);
            mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
            mapTemp.put("systemCNName", processParam.getSystemCnName());
            mapTemp.put("bureauName", processParam.getHostDeptName());
            mapTemp.put("itemId", processParam.getItemId());
            mapTemp.put("processInstanceId", processParam.getProcessInstanceId());
            mapTemp.putAll(formDataApi.getData(tenantId, processParam.getItemId(), processSerialNumber).getData());
            mapTemp.put(SysVariables.ITEMBOX, StringUtils.isBlank(processParam.getCompleter())
                ? ItemBoxTypeEnum.DOING.getValue() : ItemBoxTypeEnum.DONE.getValue());
            List<SignDeptDetailModel> signDeptDetailModels =
                signDeptDetailApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
            List<TaskModel> finalTaskList =
                taskApi.findByProcessInstanceId(tenantId, processParam.getProcessInstanceId()).getData();
            AtomicInteger count = new AtomicInteger(0);
            List<Map<String, Object>> childrenList = new ArrayList<>();
            signDeptDetailModels.forEach(sdd -> {
                List<String> taskNameAndAssigneeNames =
                    getTaskNameAndAssigneeNames(finalTaskList, sdd.getExecutionId());
                Map<String, Object> childrenMap = new HashMap<>(mapTemp);
                childrenMap.put("id", sdd.getId());
                childrenMap.put("serialNumber", count.incrementAndGet());
                childrenMap.put("taskName", taskNameAndAssigneeNames.get(0));
                childrenMap.put("taskAssignee", taskNameAndAssigneeNames.get(1));
                childrenMap.put("children", List.of());
                childrenMap.put("status", sdd.getStatus());
                childrenMap.put("bureauName", sdd.getDeptName());
                childrenList.add(childrenMap);
            });
            return Y9Result.success(childrenList, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Result.success(List.of(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> recycleList(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemRecycleApi.findByUserIdAndSystemName(tenantId, positionId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", "已办结");
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("completer",
                        StringUtils.isBlank(processParam.getCompleter()) ? "无" : processParam.getCompleter());
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DONE.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取回收站列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> recycleList4Dept(String itemId, boolean isBureau, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            Position position = Y9LoginUserHolder.getPosition();
            OrgUnit bureau = orgUnitApi.getBureau(tenantId, positionId).getData();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage;
            if (isBureau) {
                itemPage = itemRecycleApi.findByDeptIdAndSystemName(tenantId, bureau.getId(), true,
                    item.getSystemName(), page, rows);
            } else {
                itemPage = itemRecycleApi.findByDeptIdAndSystemName(tenantId, position.getParentId(), false,
                    item.getSystemName(), page, rows);
            }
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", "已办结");
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("completer",
                        StringUtils.isBlank(processParam.getCompleter()) ? "无" : processParam.getCompleter());
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DONE.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取部门回收站列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> recycleList4All(String itemId, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage =
                itemRecycleApi.findBySystemName(tenantId, item.getSystemName(), page, rows);
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                    processParam = processParamApi.findByProcessInstanceId(tenantId, processInstanceId).getData();
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", "已办结");
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("completer",
                        StringUtils.isBlank(processParam.getCompleter()) ? "无" : processParam.getCompleter());
                    mapTemp.put("taskId", taskId);
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.DONE.getValue());
                } catch (Exception e) {
                    LOGGER.error("获取回收站列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }

    @Override
    public Y9Page<Map<String, Object>> todoList(String itemId, String searchMapStr, Integer page, Integer rows) {
        try {
            String tenantId = Y9LoginUserHolder.getTenantId(), positionId = Y9LoginUserHolder.getPositionId();
            ItemModel item = itemApi.getByItemId(tenantId, itemId).getData();
            Y9Page<ActRuDetailModel> itemPage;
            if (StringUtils.isBlank(searchMapStr)) {
                itemPage =
                    itemTodoApi.findByUserIdAndSystemName(tenantId, positionId, item.getSystemName(), page, rows);
            } else {
                itemPage = itemTodoApi.searchByUserIdAndSystemName(tenantId, positionId, item.getSystemName(),
                    searchMapStr, page, rows);
            }
            List<ActRuDetailModel> list = itemPage.getRows();
            ObjectMapper objectMapper = new ObjectMapper();
            List<ActRuDetailModel> taslList = objectMapper.convertValue(list, new TypeReference<>() {});
            List<Map<String, Object>> items = new ArrayList<>();
            int serialNumber = (page - 1) * rows;
            Map<String, Object> mapTemp;
            ProcessParamModel processParam;
            String processInstanceId;
            Map<String, Object> formData;
            for (ActRuDetailModel ardModel : taslList) {
                mapTemp = new HashMap<>(16);
                String taskId = ardModel.getTaskId();
                processInstanceId = ardModel.getProcessInstanceId();
                try {
                    String processSerialNumber = ardModel.getProcessSerialNumber();
                    processParam = processParamApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
                    mapTemp.put("actRuDetailId", ardModel.getId());
                    mapTemp.put("systemCNName", processParam.getSystemCnName());
                    mapTemp.put("bureauName", processParam.getHostDeptName());
                    mapTemp.put("taskName", ardModel.getTaskDefName());
                    mapTemp.put("itemId", processParam.getItemId());
                    mapTemp.put("processInstanceId", processInstanceId);
                    mapTemp.put("taskId", taskId);
                    mapTemp.put("taskAssignee", ardModel.getAssigneeName());
                    /*
                     * 暂时取表单所有字段数据
                     */
                    formData = formDataApi.getData(tenantId, itemId, processSerialNumber).getData();
                    mapTemp.putAll(formData);
                    List<TaskRelatedModel> taskRelatedList = taskRelatedApi.findByTaskId(tenantId, taskId).getData();
                    if (ardModel.isStarted()) {
                        taskRelatedList.add(0, new TaskRelatedModel(TaskRelatedEnum.NEWTODO.getValue(), "新"));
                    }
                    /*
                     * 红绿灯
                     */
                    if (null != ardModel.getDueDate()) {
                        taskRelatedList.add(workDayService.getLightColor(new Date(), ardModel.getDueDate()));
                    }
                    taskRelatedList = taskRelatedList.stream().filter(t -> Integer.parseInt(t.getInfoType()) < Integer
                        .parseInt(TaskRelatedEnum.ACTIONNAME.getValue())).collect(Collectors.toList());
                    List<UrgeInfoModel> urgeInfoList =
                        urgeInfoApi.findByProcessSerialNumber(tenantId, processSerialNumber).getData();
                    if (ardModel.isSub()) {
                        urgeInfoList = urgeInfoList.stream().filter(
                            urgeInfo -> urgeInfo.isSub() && urgeInfo.getExecutionId().equals(ardModel.getExecutionId()))
                            .collect(Collectors.toList());
                    } else {
                        urgeInfoList =
                            urgeInfoList.stream().filter(urgeInfo -> !urgeInfo.isSub()).collect(Collectors.toList());
                    }
                    if (!urgeInfoList.isEmpty()) {
                        taskRelatedList.add(new TaskRelatedModel(TaskRelatedEnum.URGE.getValue(),
                            Y9JsonUtil.writeValueAsString(urgeInfoList)));
                    }
                    mapTemp.put(SysVariables.TASKRELATEDLIST, taskRelatedList);
                    mapTemp.put(SysVariables.ITEMBOX, ItemBoxTypeEnum.TODO.getValue());
                    mapTemp.put(SysVariables.PROCESSSERIALNUMBER, processSerialNumber);
                } catch (Exception e) {
                    LOGGER.error("获取待办列表失败" + processInstanceId, e);
                }
                mapTemp.put("serialNumber", serialNumber + 1);
                serialNumber += 1;
                items.add(mapTemp);
            }
            return Y9Page.success(page, itemPage.getTotalPages(), itemPage.getTotal(), items, "获取列表成功");
        } catch (Exception e) {
            LOGGER.error("获取待办异常", e);
        }
        return Y9Page.success(page, 0, 0, new ArrayList<>(), "获取列表失败");
    }
}
