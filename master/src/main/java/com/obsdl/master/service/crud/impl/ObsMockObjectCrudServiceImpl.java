package com.obsdl.master.service.crud.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.obsdl.master.entity.ObsMockObjectEntity;
import com.obsdl.master.mapper.ObsMockObjectMapper;
import com.obsdl.master.service.crud.ObsMockObjectCrudService;
import org.springframework.stereotype.Service;

@Service
public class ObsMockObjectCrudServiceImpl extends ServiceImpl<ObsMockObjectMapper, ObsMockObjectEntity>
        implements ObsMockObjectCrudService {
}
