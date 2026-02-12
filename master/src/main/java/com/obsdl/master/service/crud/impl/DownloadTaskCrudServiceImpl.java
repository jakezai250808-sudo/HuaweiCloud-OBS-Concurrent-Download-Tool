package com.obsdl.master.service.crud.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.obsdl.master.entity.DownloadTaskEntity;
import com.obsdl.master.mapper.DownloadTaskMapper;
import com.obsdl.master.service.crud.DownloadTaskCrudService;
import org.springframework.stereotype.Service;

@Service
public class DownloadTaskCrudServiceImpl extends ServiceImpl<DownloadTaskMapper, DownloadTaskEntity>
        implements DownloadTaskCrudService {
}
