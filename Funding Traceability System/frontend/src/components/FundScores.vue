<template>
  <el-card shadow="never" style="margin-bottom:16px">
    <div slot="header">关联阶段评分 <el-button type="text" style="float:right;padding:0" @click="$router.push({ path: '/score', query: { fundId } })">查看全部</el-button></div>
    <el-table v-loading="loading" :data="records" border size="small">
      <el-table-column prop="id" label="评分ID" width="90" />
      <el-table-column prop="scoreDate" label="评分日期" />
      <el-table-column prop="totalScore" label="总分" />
      <el-table-column label="评分规则"><template slot-scope="{ row }">{{ row.ruleVersion ? row.ruleVersion.slice(0, 12) : '历史规则未留存' }}</template></el-table-column>
    </el-table>
    <div style="margin-top:8px;color:#65758b">共{{ total }}条，展示最近10条；未绑定资助单的历史评分不计入此列表。</div>
  </el-card>
</template>
<script>
import { listScores } from '@/api/score'
export default {
  name: 'FundScores',
  props: { fundId: { type: [String, Number], required: true } },
  data() { return { records: [], total: 0, loading: false } },
  watch: { fundId: { immediate: true, handler: 'load' } },
  methods: {
    load() {
      const id = this.fundId
      this.loading = true
      listScores({ page: 1, size: 10, fundId: id }).then(data => {
        if (id !== this.fundId) return
        this.records = data.records || []
        this.total = data.total || 0
      }).finally(() => { if (id === this.fundId) this.loading = false })
    }
  }
}
</script>
