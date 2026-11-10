package com.funding.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.funding.entity.OperationLogEntity;
import com.funding.entity.TimestampRecord;
import com.funding.vo.FundVO;
import com.funding.vo.ScoreVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadOnlyAuditServiceTest {
    @Mock private FundService funds;
    @Mock private ScoreService scores;
    @Mock private TimestampService timestamps;
    @Mock private OperationLogService logs;
    @InjectMocks private ReadOnlyAuditService service;
    @Test void fundProjectionNeverReturnsFreeTextOrNamesOfSponsors() {
        FundVO f=new FundVO();f.setId(1L);f.setRemark("PRIVATE");f.setPurpose("PRIVATE");f.setAuditRemark("PRIVATE");f.setSponsorName("PRIVATE");
        when(funds.getById(1L)).thenReturn(f);
        Map<String,Object> data=service.fund(1L);
        assertEquals(1L,data.get("id")); assertFalse(data.containsValue("PRIVATE"));assertFalse(data.containsKey("remark"));
    }
    @Test void scoreDetailsAreReadWithoutReturningRemarks() {
        ScoreVO s=new ScoreVO();s.setId(1L);s.setRemark("PRIVATE");s.setSponsorName("PRIVATE");s.setDetails(Collections.emptyList());
        when(scores.getById(1L)).thenReturn(s);
        Map<String,Object> data=service.score(1L);assertFalse(data.containsValue("PRIVATE"));assertTrue(data.containsKey("details"));
    }
    @Test void logProjectionOmitsTheOriginalParameterContent() {
        OperationLogEntity entry=new OperationLogEntity();entry.setContent("PRIVATE");entry.setOperation("审核申请");
        Page<OperationLogEntity> page=new Page<>();page.setRecords(Collections.singletonList(entry));
        when(logs.page(anyInt(),anyInt(),isNull(),isNull(),isNull())).thenReturn(page);
        Map<String,Object> result=service.logs(1,10).getRecords().get(0);
        assertEquals("审核申请",result.get("operation"));assertFalse(result.containsKey("content"));assertFalse(result.containsValue("PRIVATE"));
    }
    @Test void verificationDoesNotLeakNodeSummaries() {
        Map<String,Object> raw=new HashMap<>();raw.put("valid",true);raw.put("nodes",Collections.singletonList("PRIVATE"));
        when(timestamps.verify(null,null)).thenReturn(raw);
        assertEquals(Boolean.TRUE,service.verify(null).get("valid"));assertFalse(service.verify(null).containsKey("nodes"));
    }
}
