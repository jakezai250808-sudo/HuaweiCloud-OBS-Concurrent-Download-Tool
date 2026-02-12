package com.obsdl.master.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.obsdl.master.entity.TaskObjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TaskObjectMapper extends BaseMapper<TaskObjectEntity> {

    @Select("""
            <script>
            SELECT id, task_id, object_key, size, status, leased_by, etag, created_at, updated_at
            FROM task_object
            WHERE task_id = #{taskId}
              AND status = 'PENDING'
            ORDER BY id
            LIMIT #{count}
            FOR UPDATE
            </script>
            """)
    List<TaskObjectEntity> selectPendingForUpdate(@Param("taskId") Long taskId, @Param("count") Integer count);

    @Update("""
            <script>
            UPDATE task_object
            SET status = 'LEASED',
                leased_by = #{workerId},
                updated_at = NOW()
            WHERE task_id = #{taskId}
              AND id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>
                  #{id}
              </foreach>
            </script>
            """)
    int leaseObjects(@Param("taskId") Long taskId, @Param("ids") List<Long> ids, @Param("workerId") String workerId);

    @Select("""
            SELECT id, task_id, object_key, size, status, leased_by, etag, created_at, updated_at
            FROM task_object
            WHERE task_id = #{taskId}
              AND object_key = #{objectKey}
            FOR UPDATE
            """)
    TaskObjectEntity selectByTaskIdAndObjectKeyForUpdate(@Param("taskId") Long taskId,
                                                          @Param("objectKey") String objectKey);
}
