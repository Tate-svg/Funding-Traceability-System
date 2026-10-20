package com.funding.common;

import com.funding.entity.Fund;
import com.funding.entity.Student;
import com.funding.entity.TimestampRecord;
import com.funding.entity.Visit;
import com.funding.mapper.FundMapper;
import com.funding.mapper.StudentMapper;
import com.funding.mapper.TimestampRecordMapper;
import com.funding.mapper.VisitMapper;
import com.funding.service.TimestampService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 存量数据可信时间戳补盖
 * 仅在 timestamp_record 空表时执行一次（重跑 schema.sql 后自动重建演示链）；
 * 按业务时间（提交/审核/拨付/回访日期）升序依次盖章，保证哈希链与时序一致
 */
@Slf4j
@Component
public class SeedTimestampRunner implements ApplicationRunner {

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private TimestampRecordMapper timestampRecordMapper;

    @Autowired
    private FundMapper fundMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private VisitMapper visitMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (timestampRecordMapper.countAll() > 0) {
            return;
        }
        List<Object[]> stamps = new ArrayList<>();
        // 结构：{node, nodeName, bizType, bizId, summary, operatorName, role, tsTime}

        // 学生资料上传
        for (Student s : studentMapper.selectList(null)) {
            stamps.add(new Object[]{"STUDENT_UPLOAD", "资料上传", "student", s.getId(),
                    "学生资料上传 #" + s.getId(), "audit_admin", "AUDIT_ADMIN", s.getCreateTime()});
        }
        // 回访录入
        for (Visit v : visitMapper.selectList(null)) {
            LocalDateTime time = v.getVisitDate() != null ? v.getVisitDate().atStartOfDay() : null;
            stamps.add(new Object[]{"VISIT_RECORD", "回访录入", "visit", v.getId(),
                    "回访录入 #" + v.getId(), "sponsor", "SPONSOR", time});
        }
        // 资金单各节点：提交 → 审核通过 → 拨付
        for (Fund f : fundMapper.selectList(null)) {
            if (f.getCreateTime() != null) {
                stamps.add(new Object[]{"SUBMIT", "提交申请", "fund", f.getId(),
                        "提交申请 #" + f.getId(), "sponsor", "SPONSOR", f.getCreateTime()});
            }
            if (f.getAuditTime() != null) {
                stamps.add(new Object[]{"AUDIT_PASS", "审核通过", "fund", f.getId(),
                        "审核通过 #" + f.getId(), "audit_admin", "AUDIT_ADMIN", f.getAuditTime()});
            }
            if (f.getDisburseTime() != null) {
                stamps.add(new Object[]{"DISBURSE", "资金拨付", "fund", f.getId(),
                        "资金拨付 #" + f.getId(), "disburse_admin", "DISBURSE_ADMIN", f.getDisburseTime()});
            }
        }

        // 按业务时间升序依次盖章，serial 与哈希链随之自然固化
        stamps.sort(Comparator.comparing(o -> ((LocalDateTime) o[7]) == null
                ? LocalDateTime.MAX : (LocalDateTime) o[7]));
        for (Object[] s : stamps) {
            LocalDateTime time = (LocalDateTime) s[7] != null ? (LocalDateTime) s[7] : LocalDateTime.now();
            timestampService.stampSeed((String) s[0], (String) s[1], (String) s[2], (Long) s[3],
                    (String) s[4], (String) s[5], (String) s[6], time);
        }
        log.info("存量数据可信时间戳补盖完成，共 {} 条", stamps.size());
    }
}
