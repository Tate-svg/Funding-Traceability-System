package com.funding.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.TimestampRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 可信时间戳台账 Mapper
 * 硬性约定：不继承 BaseMapper，仅提供 insert 与查询，
 * 从结构上杜绝 update / delete，保证时间戳台账只读不可篡改
 */
public interface TimestampRecordMapper {

    @Insert("INSERT INTO timestamp_record (serial_no, biz_type, biz_id, node, node_name, summary, " +
            "operator_id, operator_name, role, ts_time, ts_hash, prev_hash, anomaly, anomaly_reason, create_time) " +
            "VALUES (#{serialNo}, #{bizType}, #{bizId}, #{node}, #{nodeName}, #{summary}, " +
            "#{operatorId}, #{operatorName}, #{role}, #{tsTime}, #{tsHash}, #{prevHash}, #{anomaly}, #{anomalyReason}, NOW())")
    int insert(TimestampRecord record);

    @Select("<script>" +
            "SELECT * FROM timestamp_record " +
            "<where>" +
            "<if test='bizType != null and bizType != \"\"'>" +
            "AND biz_type = #{bizType}" +
            "</if>" +
            "<if test='bizId != null'>" +
            "AND biz_id = #{bizId}" +
            "</if>" +
            "<if test='node != null and node != \"\"'>" +
            "AND node = #{node}" +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (node_name LIKE CONCAT('%',#{keyword},'%') OR summary LIKE CONCAT('%',#{keyword},'%') OR operator_name LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY serial_no DESC" +
            "</script>")
    IPage<TimestampRecord> selectPage(Page<TimestampRecord> page,
                                      @Param("bizType") String bizType,
                                      @Param("bizId") Long bizId,
                                      @Param("node") String node,
                                      @Param("keyword") String keyword);

    /** 取某业务单据最新一条时间戳（时序倒序检测用） */
    @Select("SELECT * FROM timestamp_record WHERE biz_type = #{bizType} AND biz_id = #{bizId} " +
            "ORDER BY serial_no DESC LIMIT 1")
    TimestampRecord selectLast(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    /** 取全局链末条（计算 prev_hash 用） */
    @Select("SELECT * FROM timestamp_record ORDER BY serial_no DESC LIMIT 1")
    TimestampRecord selectLastGlobal();

    /** 全链按序号升序（核验重算用） */
    @Select("SELECT * FROM timestamp_record ORDER BY serial_no ASC")
    List<TimestampRecord> selectAllOrdered();

    /** 取某业务单据全部时间戳（按序号升序） */
    @Select("SELECT * FROM timestamp_record WHERE biz_type = #{bizType} AND biz_id = #{bizId} " +
            "ORDER BY serial_no ASC")
    List<TimestampRecord> selectListByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Select("SELECT COUNT(*) FROM timestamp_record")
    long countAll();

    /** 当前全局最大序列号 */
    @Select("SELECT COALESCE(MAX(serial_no), 0) FROM timestamp_record")
    long maxSerialNo();
}
