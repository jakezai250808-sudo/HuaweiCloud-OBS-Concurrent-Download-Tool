package com.obsdl.master.service.crud.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.obsdl.master.entity.ObsAccountEntity;
import com.obsdl.master.mapper.ObsAccountMapper;
import com.obsdl.master.service.crud.ObsAccountCrudService;
import org.springframework.stereotype.Service;

@Service
public class ObsAccountCrudServiceImpl extends ServiceImpl<ObsAccountMapper, ObsAccountEntity>
        implements ObsAccountCrudService {
}
