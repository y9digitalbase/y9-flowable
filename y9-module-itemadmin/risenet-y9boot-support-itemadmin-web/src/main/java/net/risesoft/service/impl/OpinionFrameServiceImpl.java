package net.risesoft.service.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.risesoft.entity.ItemOpinionFrameBind;
import net.risesoft.entity.OpinionFrame;
import net.risesoft.id.IdType;
import net.risesoft.id.Y9IdGenerator;
import net.risesoft.model.user.UserInfo;
import net.risesoft.repository.jpa.OpinionFrameRepository;
import net.risesoft.service.ItemOpinionFrameBindService;
import net.risesoft.service.OpinionFrameService;
import net.risesoft.y9.Y9LoginUserHolder;

/**
 * @author qinman
 * @author zhangchongjie
 * @date 2022/12/22
 */
@Transactional(value = "rsTenantTransactionManager", readOnly = true)
@Service(value = "opinionFrameService")
public class OpinionFrameServiceImpl implements OpinionFrameService {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OpinionFrameRepository opinionFrameRepository;

    @Autowired
    private ItemOpinionFrameBindService itemOpinionFrameBindService;

    @Override
    public List<OpinionFrame> findAll() {
        Sort sort = Sort.by(Sort.Direction.ASC, "createDate");
        return opinionFrameRepository.findAll(sort);
    }

    @SuppressWarnings("serial")
    @Override
    public Page<OpinionFrame> findAll(int page, int rows) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createDate");
        PageRequest pageable = PageRequest.of(page > 0 ? page - 1 : 0, rows, sort);
        return opinionFrameRepository.findAll(new Specification<OpinionFrame>() {

            @Override
            public Predicate toPredicate(Root<OpinionFrame> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                List<Predicate> list = new ArrayList<Predicate>();
                Predicate[] predicates = new Predicate[list.size()];
                list.toArray(predicates);
                return builder.and(predicates);
            }
        }, pageable);
    }

    @Override
    public Page<OpinionFrame> findAllNotUsed(String itemId, String processDefinitionId, String taskDefKey, int page,
        int rows) {
        List<ItemOpinionFrameBind> bindList = itemOpinionFrameBindService
            .findByItemIdAndProcessDefinitionIdAndTaskDefKey(itemId, processDefinitionId, taskDefKey);
        List<String> markList = new ArrayList<>();
        for (ItemOpinionFrameBind bind : bindList) {
            markList.add(bind.getOpinionFrameMark());
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "createDate");
        PageRequest pageable = PageRequest.of(page > 0 ? page - 1 : 0, rows, sort);
        if (markList.isEmpty()) {
            return opinionFrameRepository.findAll(pageable);
        } else {
            return opinionFrameRepository.findByMarkNotIn(markList, pageable);
        }
    }

    @Override
    public OpinionFrame findByMark(String mark) {
        return opinionFrameRepository.findByMark(mark);
    }

    @Override
    public OpinionFrame findOne(String id) {
        return opinionFrameRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = false)
    public void remove(String id) {
        OpinionFrame opinionFrame = this.findOne(id);
        if (opinionFrame != null) {
            List<ItemOpinionFrameBind> list = itemOpinionFrameBindService.findByMark(opinionFrame.getMark());
            for (ItemOpinionFrameBind bind : list) {
                itemOpinionFrameBindService.delete(bind.getId());
            }
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void remove(String[] ids) {
        for (String id : ids) {
            this.remove(id);
            opinionFrameRepository.deleteById(id);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public OpinionFrame save(OpinionFrame opinionFrame) {
        return opinionFrameRepository.save(opinionFrame);
    }

    @Override
    @Transactional(readOnly = false)
    public OpinionFrame saveOrUpdate(OpinionFrame opinionFrame) {
        try {
            UserInfo userInfo = Y9LoginUserHolder.getUserInfo();
            String id = opinionFrame.getId();
            if (StringUtils.isNotEmpty(id)) {
                OpinionFrame oldof = this.findOne(id);
                if (null != oldof) {
                    oldof.setModifyDate(sdf.format(new Date()));
                    oldof.setName(opinionFrame.getName());
                    oldof.setMark(opinionFrame.getMark());
                    oldof.setUserId(null == userInfo ? "" : userInfo.getPersonId());
                    oldof.setUserName(null == userInfo ? "" : userInfo.getName());
                    this.save(oldof);
                    return oldof;
                } else {
                    return this.save(opinionFrame);
                }
            }

            OpinionFrame newof = new OpinionFrame();
            newof.setId(Y9IdGenerator.genId(IdType.SNOWFLAKE));
            newof.setMark(opinionFrame.getMark());
            newof.setCreateDate(sdf.format(new Date()));
            newof.setModifyDate(sdf.format(new Date()));
            newof.setName(opinionFrame.getName());
            newof.setTenantId(userInfo.getTenantId());
            newof.setUserId(userInfo.getPersonId());
            newof.setUserName(userInfo.getName());
            newof.setDeleted(0);
            this.save(newof);

            return newof;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("serial")
    @Override
    public Page<OpinionFrame> search(int page, int rows, final String keyword) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createDate");
        PageRequest pageable = PageRequest.of(page > 0 ? page - 1 : 0, rows, sort);
        return opinionFrameRepository.findAll(new Specification<OpinionFrame>() {

            @Override
            public Predicate toPredicate(Root<OpinionFrame> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                List<Predicate> list = new ArrayList<Predicate>();
                Path<String> nameExp = root.get("name");
                if (StringUtils.isNotEmpty(keyword)) {
                    list.add(builder.like(nameExp, "%" + keyword + "%"));
                }
                Predicate[] predicates = new Predicate[list.size()];
                list.toArray(predicates);
                return builder.and(predicates);
            }
        }, pageable);
    }

    @Override
    public Page<OpinionFrame> search4NotUsed(String itemId, String processDefinitionId, String taskDefKey, int page,
        int rows, final String keyword) {
        List<ItemOpinionFrameBind> bindList = itemOpinionFrameBindService
            .findByItemIdAndProcessDefinitionIdAndTaskDefKey(itemId, processDefinitionId, taskDefKey);
        List<String> markList = new ArrayList<>();
        for (ItemOpinionFrameBind bind : bindList) {
            markList.add(bind.getOpinionFrameMark());
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "createDate");
        PageRequest pageable = PageRequest.of(page > 0 ? page - 1 : 0, rows, sort);
        if (markList.isEmpty()) {
            if (StringUtils.isEmpty(keyword)) {
                return opinionFrameRepository.findAll(pageable);
            } else {
                return opinionFrameRepository.findByNameContaining(keyword, pageable);
            }
        } else {
            if (StringUtils.isEmpty(keyword)) {
                return opinionFrameRepository.findByMarkNotIn(markList, pageable);
            } else {
                return opinionFrameRepository.findByMarkNotInAndNameLike(markList, "%" + keyword + "%", pageable);
            }
        }
    }
}