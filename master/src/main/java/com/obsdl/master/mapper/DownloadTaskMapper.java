package com.obsdl.master.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.obsdl.master.entity.DownloadTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DownloadTaskMapper extends BaseMapper<DownloadTaskEntity> {

    @Update("""
            UPDATE download_task
            SET done_objects = done_objects + 1,
                updated_at = NOW()
            WHERE id = #{taskId}
            """)
    int increaseDoneObjects(@Param("taskId") Long taskId);

    @Update("""
            UPDATE download_task
            SET status = 'DONE',
                updated_at = NOW()
            WHERE id = #{taskId}
              AND done_objects = total_objects
              AND status <> 'DONE'
            """)
    int markTaskDoneIfFinished(@Param("taskId") Long taskId);
}
