package com.funding.service.impl;

import com.funding.common.BusinessException;
import com.funding.entity.*;
import com.funding.mapper.*;
import com.funding.vo.ScoreVO;
import com.funding.vo.ScoreDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceImplTest {
    @Mock private ScoreMapper scoreMapper;
    @Mock private ScoreDetailMapper detailMapper;
    @Mock private ScoreItemMapper itemMapper;
    @Mock private FundMapper fundMapper;
    @InjectMocks private ScoreServiceImpl service;
    private List<ScoreItem> rules;

    private ScoreItem item(long id, String weight) {
        ScoreItem i = new ScoreItem(); i.setId(id); i.setName("指标" + id); i.setWeight(new BigDecimal(weight)); return i;
    }
    private ScoreDetailVO detail(long id, String value) {
        ScoreDetailVO d = new ScoreDetailVO(); d.setItemId(id);
        d.setItemScore(value == null ? null : new BigDecimal(value)); return d;
    }
    private ScoreVO request(ScoreDetailVO... details) {
        ScoreVO vo = new ScoreVO(); vo.setId(1L); vo.setFundId(10L); vo.setStudentId(2L); vo.setSponsorId(3L);
        vo.setDetails(Arrays.asList(details)); vo.setTotalScore(new BigDecimal("999")); return vo;
    }
    @BeforeEach void setUp() {
        rules = Arrays.asList(item(1, "30"), item(2, "70"));
        lenient().when(itemMapper.selectList(isNull())).thenReturn(rules);
        Fund f = new Fund(); f.setId(10L); f.setStudentId(2L); f.setSponsorId(3L);
        lenient().when(fundMapper.selectOne(any())).thenReturn(f);
        lenient().doAnswer(inv -> { ((Score) inv.getArgument(0)).setId(1L); return 1; }).when(scoreMapper).insert(any());
    }
    private void noWrites() {
        verify(scoreMapper, never()).insert(any()); verify(scoreMapper, never()).updateById(any());
        verify(detailMapper, never()).insert(any()); verify(detailMapper, never()).delete(any());
    }
    private void rejectsWithoutWrites(ScoreVO vo) { assertThrows(BusinessException.class, () -> service.saveScore(vo)); noWrites(); }
    @Test void missingItemMustNotBecomeZero() { rejectsWithoutWrites(request(detail(1, "90"))); }
    @Test void nullScoreIsMissing() { rejectsWithoutWrites(request(detail(1, null), detail(2, "90"))); }
    @Test void nullDetailsAreRejected() { ScoreVO vo = request(); vo.setDetails(null); rejectsWithoutWrites(vo); }
    @Test void duplicateItemMustNotInflateTotal() { rejectsWithoutWrites(request(detail(1, "90"), detail(1, "90"))); }
    @Test void unknownItemIsRejected() { rejectsWithoutWrites(request(detail(1, "90"), detail(3, "90"))); }
    @Test void outOfRangeScoresAreRejected() {
        rejectsWithoutWrites(request(detail(1, "-1"), detail(2, "90")));
        rejectsWithoutWrites(request(detail(1, "101"), detail(2, "90")));
    }
    @Test void weightsMustSumToOneHundred() {
        when(itemMapper.selectList(isNull())).thenReturn(Arrays.asList(item(1, "30"), item(2, "60")));
        rejectsWithoutWrites(request(detail(1, "80"), detail(2, "90")));
    }
    @Test void negativeWeightsAreRejectedEvenWhenSumIsOneHundred() {
        when(itemMapper.selectList(isNull())).thenReturn(Arrays.asList(item(1, "-10"), item(2, "110")));
        rejectsWithoutWrites(request(detail(1, "80"), detail(2, "90")));
    }
    @Test void noConfiguredItemsCannotProduceAZeroScore() {
        when(itemMapper.selectList(isNull())).thenReturn(Collections.emptyList()); rejectsWithoutWrites(request());
    }
    @Test void actualZeroAndServerSnapshotIgnoreClientMetadata() {
        ScoreVO input=request(detail(1,"0"),detail(2,"80")); input.setRuleVersion("fake");
        input.getDetails().get(0).setWeight(new BigDecimal("999")); input.getDetails().get(0).setItemName("伪造指标");
        service.saveScore(input);
        ArgumentCaptor<Score> header=ArgumentCaptor.forClass(Score.class); verify(scoreMapper).insert(header.capture());
        assertEquals(new BigDecimal("56.00"),header.getValue().getTotalScore());
        assertNotEquals("fake",header.getValue().getRuleVersion()); assertEquals(64,header.getValue().getRuleVersion().length());
        ArgumentCaptor<ScoreDetail> ds=ArgumentCaptor.forClass(ScoreDetail.class); verify(detailMapper,times(2)).insert(ds.capture());
        assertEquals("指标1",ds.getAllValues().get(0).getItemNameSnapshot());
        assertEquals(new BigDecimal("30"),ds.getAllValues().get(0).getWeightSnapshot());
    }
    private Score savedSnapshot() {
        service.saveScore(request(detail(1,"0"),detail(2,"80")));
        ArgumentCaptor<Score> header=ArgumentCaptor.forClass(Score.class); verify(scoreMapper).insert(header.capture());
        ArgumentCaptor<ScoreDetail> ds=ArgumentCaptor.forClass(ScoreDetail.class); verify(detailMapper,times(2)).insert(ds.capture());
        when(scoreMapper.selectOne(any())).thenReturn(header.getValue());
        when(detailMapper.selectList(any())).thenReturn(ds.getAllValues());
        clearInvocations(scoreMapper,detailMapper,itemMapper,fundMapper);
        return header.getValue();
    }
    @Test void updateRejectsIncompleteInputBeforeChangingExistingData() {
        savedSnapshot(); assertThrows(BusinessException.class,()->service.updateScore(request(detail(1,"90")))); noWrites();
    }
    @Test void editingUsesFrozenRulesEvenWhenCurrentItemsAreGone() {
        Score old=savedSnapshot(); lenient().when(itemMapper.selectList(isNull())).thenReturn(Collections.emptyList());
        ScoreVO vo=request(detail(1,"100"),detail(2,"0")); service.updateScore(vo);
        ArgumentCaptor<Score> update=ArgumentCaptor.forClass(Score.class); verify(scoreMapper).updateById(update.capture());
        assertEquals(new BigDecimal("30.00"),update.getValue().getTotalScore());
        assertNull(update.getValue().getRuleVersion()); // SQL只更新得分等字段，原规则标识不动。
        assertNotNull(old.getRuleVersion()); verifyNoInteractions(itemMapper);
    }
    @Test void corruptedSnapshotsAreRejectedWithoutRecalculation() {
        Score old=savedSnapshot(); old.setRuleVersion("corrupt");
        assertThrows(BusinessException.class,()->service.updateScore(request(detail(1,"10"),detail(2,"20")))); noWrites();
    }
    @Test void fundIsMandatory() { ScoreVO v=request(); v.setFundId(null); rejectsWithoutWrites(v); }
    @Test void fundMustExist() { when(fundMapper.selectOne(any())).thenReturn(null); rejectsWithoutWrites(request()); }
    @Test void fundMustMatchStudent() { ScoreVO v=request(); v.setStudentId(99L); rejectsWithoutWrites(v); }
    @Test void fundMustMatchSponsor() { ScoreVO v=request(); v.setSponsorId(99L); rejectsWithoutWrites(v); }
    @Test void legacyMetadataCanChangeWithoutInventingHistoricalRules() {
        Score old=new Score(); old.setId(1L); old.setStudentId(2L); old.setSponsorId(3L); old.setTotalScore(new BigDecimal("88.8"));
        when(scoreMapper.selectOne(any())).thenReturn(old); when(detailMapper.selectList(any())).thenReturn(Collections.emptyList());
        ScoreVO vo=request(); vo.setDetails(null); vo.setRemark("补充关联"); service.updateScore(vo);
        ArgumentCaptor<Score> update=ArgumentCaptor.forClass(Score.class); verify(scoreMapper).updateById(update.capture());
        assertNull(update.getValue().getTotalScore()); assertNull(update.getValue().getRuleVersion()); assertEquals(10L,update.getValue().getFundId());
        verify(detailMapper,never()).delete(any()); verify(detailMapper,never()).insert(any()); verifyNoInteractions(itemMapper);
    }
    @Test void legacyScoresCannotBeEditedIntoCurrentRules() {
        Score old=new Score(); old.setId(1L); old.setStudentId(2L); old.setSponsorId(3L);
        when(scoreMapper.selectOne(any())).thenReturn(old); when(detailMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class,()->service.updateScore(request(detail(1,"50")))); noWrites();
    }
}
