package com.obsdl.master.service.crud.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.obsdl.master.entity.WorkerNodeEntity;
import com.obsdl.master.mapper.WorkerNodeMapper;
import com.obsdl.master.service.crud.WorkerNodeCrudService;
import org.springframework.stereotype.Service;

@Service
public class WorkerNodeCrudServiceImpl extends ServiceImpl<WorkerNodeMapper, WorkerNodeEntity>
        implements WorkerNodeCrudService {
}
