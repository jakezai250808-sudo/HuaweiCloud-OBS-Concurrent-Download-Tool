package com.obsdl.master.service.crud.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.obsdl.master.entity.TaskObjectEntity;
import com.obsdl.master.mapper.TaskObjectMapper;
import com.obsdl.master.service.crud.TaskObjectCrudService;
import org.springframework.stereotype.Service;

@Service
public class TaskObjectCrudServiceImpl extends ServiceImpl<TaskObjectMapper, TaskObjectEntity>
        implements TaskObjectCrudService {
}
