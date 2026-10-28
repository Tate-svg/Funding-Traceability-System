<template>
  <div class="page-container dashboard-container">
    <div class="page-header">
      <div>
        <div class="page-title">首页</div>
        <div class="page-subtitle">我资助的学生、资金、回访与评分数据总览</div>
      </div>
      <div class="page-actions">
        <span class="data-pill"><i class="el-icon-data-analysis"></i> 数据实时汇总</span>
      </div>
    </div>

    <!-- 数据卡片 -->
    <el-row :gutter="16" class="card-row">
      <el-col :span="6" v-for="card in cards" :key="card.key">
        <div class="stat-card" :style="{ borderTopColor: card.color }">
          <div class="stat-icon" :style="{ background: card.color + '22', color: card.color }">
            <i :class="card.icon"></i>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">各模块数量</div>
          <div ref="moduleBar" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">月度资金趋势</div>
          <div ref="monthlyLine" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">资金用途分布 <span class="chart-hint">按 7 类标准用途归类</span></div>
          <div ref="purposePie" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">各评分指标均值</div>
          <div ref="scoreAvgBar" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/dashboard'

export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {},
      charts: []
    }
  },
  computed: {
    cards() {
      const s = this.stats || {}
      return [
        { key: 'student', label: '学生总数', value: s.studentCount || 0, icon: 'el-icon-user', color: '#2b6cb0' },
        { key: 'fundAmount', label: '资助总金额(元)', value: this.formatAmount(s.fundTotalAmount), icon: 'el-icon-money', color: '#2f9e6e' },
        { key: 'fundCount', label: '资助次数', value: s.fundCount || 0, icon: 'el-icon-wallet', color: '#c07f2e' },
        { key: 'visit', label: '回访次数', value: s.visitCount || 0, icon: 'el-icon-chat-line-round', color: '#2f8f9e' }
      ]
    }
  },
  mounted() {
    this.fetchData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.charts.forEach(c => c && c.dispose())
    this.charts = []
  },
  methods: {
    formatAmount(val) {
      if (val === undefined || val === null) return 0
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    fetchData() {
      getDashboardStats().then(data => {
        this.stats = data || {}
        this.$nextTick(() => {
          this.renderModuleBar()
          this.renderMonthlyLine()
          this.renderPurposePie()
          this.renderScoreAvgBar()
        })
      }).catch(() => {})
    },
    safeChart(refName) {
      const dom = this.$refs[refName]
      if (!dom) return null
      let chart = echarts.getInstanceByDom(dom)
      if (!chart) {
        chart = echarts.init(dom)
        this.charts.push(chart)
      }
      return chart
    },
    renderModuleBar() {
      const chart = this.safeChart('moduleBar')
      if (!chart) return
      const mb = this.stats.moduleBar || {}
      const data = [
        { name: '学生', value: mb.student || 0 },
        { name: '资金', value: mb.fund || 0 },
        { name: '回访', value: mb.visit || 0 },
        { name: '评分', value: mb.score || 0 }
      ]
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 40, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: data.map(d => d.name) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'bar',
          data: data.map(d => d.value),
          barWidth: '40%',
          itemStyle: { color: '#2b6cb0', borderRadius: [4, 4, 0, 0] }
        }]
      })
    },
    renderMonthlyLine() {
      const chart = this.safeChart('monthlyLine')
      if (!chart) return
      const list = this.stats.monthlyFunds || []
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: list.map(d => d.month), boundaryGap: false },
        yAxis: { type: 'value' },
        series: [{
          type: 'line',
          data: list.map(d => d.amount),
          smooth: true,
          areaStyle: { color: 'rgba(43,108,176,0.15)' },
          itemStyle: { color: '#2b6cb0' }
        }]
      })
    },
    renderPurposePie() {
      const chart = this.safeChart('purposePie')
      if (!chart) return
      const list = this.stats.purposeDistribution || []
      chart.setOption({
        color: ['#2b6cb0', '#2f9e6e', '#c07f2e', '#2f8f9e', '#6b5bb5', '#a88523', '#c24a45'],
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll', itemWidth: 10, itemHeight: 10, textStyle: { color: '#5b6b7c', fontSize: 12 } },
        series: [{
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['50%', '45%'],
          data: list.map(d => ({ name: d.name, value: d.value })),
          itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{d}%' }
        }]
      }, true)
    },
    renderScoreAvgBar() {
      const chart = this.safeChart('scoreAvgBar')
      if (!chart) return
      const list = this.stats.scoreAverages || []
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 50, right: 20, top: 30, bottom: 40 },
        xAxis: { type: 'category', data: list.map(d => d.itemName), axisLabel: { interval: 0, rotate: list.length > 4 ? 25 : 0 } },
        yAxis: { type: 'value', max: 100 },
        series: [{
          type: 'bar',
          data: list.map(d => Number(d.avg).toFixed(2)),
          barWidth: '40%',
          itemStyle: { color: '#2f9e6e', borderRadius: [4, 4, 0, 0] }
        }]
      })
    },
    handleResize() {
      this.charts.forEach(c => c && c.resize())
    }
  }
}
</script>

<style scoped>
.data-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  background: var(--c-primary-light);
  border: 1px solid #cfe0f3;
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 600;
}
.card-row { margin-bottom: 16px; }
.stat-card {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-top: 3px solid var(--c-primary);
  border-radius: var(--r-panel);
  padding: 16px 18px;
  display: flex;
  align-items: center;
  box-shadow: var(--sh-1);
  transition: box-shadow var(--t-fast), transform var(--t-fast);
}
.stat-card:hover { box-shadow: var(--sh-2); transform: translateY(-1px); }
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--r-btn);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-right: 12px;
  flex-shrink: 0;
}
.stat-body { flex: 1; min-width: 0; }
.stat-value {
  font-size: 23px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--c-text-1);
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 12px;
  color: var(--c-text-3);
  margin-top: 3px;
}
.chart-row { margin-bottom: 16px; }
.chart-card {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-radius: var(--r-panel);
  padding: 13px 16px;
  box-shadow: var(--sh-1);
}
.chart-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--c-text-1);
  margin-bottom: 8px;
  border-left: 3px solid var(--c-primary);
  padding-left: 8px;
  line-height: 1.2;
}
.chart-box {
  width: 100%;
  height: 280px;
}
.chart-hint {
  margin-left: 8px;
  font-size: 11.5px;
  font-weight: 400;
  color: var(--c-text-4);
}
@media (max-width: 768px) {
  .dashboard-container ::v-deep .el-col {
    width: 100% !important;
    max-width: 100% !important;
    flex: 0 0 100% !important;
  }
  .card-row { margin-bottom: 12px; }
  .stat-card { padding: 12px 14px; }
  .stat-value { font-size: 20px; }
  .chart-box { height: 240px; }
}
</style>
