package com.funding.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作日志 Mapper
 * 硬性约定：不继承 BaseMapper，仅提供 insert 与 selectPage，
 * 从结构上杜绝 update / delete，保证日志只读不可篡改
 */
public interface OperationLogMapper {

    @Insert("INSERT INTO operation_log (user_id, username, role, operation, target_type, target_id, content, create_time) " +
            "VALUES (#{userId}, #{username}, #{role}, #{operation}, #{targetType}, #{targetId}, #{content}, NOW())")
    int insert(OperationLogEntity log);

    @Select("<script>" +
            "SELECT * FROM operation_log " +
            "<where>" +
            "<if test='targetType != null and targetType != \"\"'>" +
            "AND target_type = #{targetType}" +
            "</if>" +
            "<if test='targetId != null'>" +
            "AND target_id = #{targetId}" +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%',#{keyword},'%') OR operation LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC" +
            "</script>")
    IPage<OperationLogEntity> selectPage(Page<OperationLogEntity> page,
                                         @Param("targetType") String targetType,
                                         @Param("targetId") Long targetId,
                                         @Param("keyword") String keyword);
}
