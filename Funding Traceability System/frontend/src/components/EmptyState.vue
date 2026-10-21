<template>
  <!-- 业务向简约线性空状态插画，替代 Element 默认插画 -->
  <div class="empty-state">
    <svg class="empty-svg" viewBox="0 0 120 96" fill="none" xmlns="http://www.w3.org/2000/svg">
      <!-- 待办清单 -->
      <template v-if="type === 'todo'">
        <rect x="30" y="14" width="60" height="74" rx="8" stroke="currentColor" stroke-width="2" />
        <rect x="42" y="8" width="36" height="12" rx="4" fill="#fff" stroke="currentColor" stroke-width="2" />
        <path d="M43 38h34M43 52h34M43 66h22" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        <path d="M34 38l3 3 5-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        <circle cx="36.5" cy="52" r="2.5" stroke="currentColor" stroke-width="2" />
      </template>
      <!-- 台账 / 凭证 -->
      <template v-else-if="type === 'ledger'">
        <rect x="24" y="18" width="72" height="64" rx="8" stroke="currentColor" stroke-width="2" />
        <path d="M24 34h72" stroke="currentColor" stroke-width="2" />
        <path d="M38 48h44M38 60h30M38 70h20" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        <circle cx="80" cy="70" r="8" fill="#fff" stroke="currentColor" stroke-width="2" />
        <path d="M77 70l2 2 4-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      </template>
      <!-- 评分 / 数据 -->
      <template v-else-if="type === 'chart'">
        <rect x="22" y="20" width="76" height="60" rx="8" stroke="currentColor" stroke-width="2" />
        <path d="M36 66V50M52 66V42M68 66V56M84 66V34" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
        <path d="M34 28h20" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      </template>
      <!-- 学生 / 档案 -->
      <template v-else-if="type === 'student'">
        <circle cx="60" cy="40" r="14" stroke="currentColor" stroke-width="2" />
        <path d="M34 84c0-14 12-22 26-22s26 8 26 22" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        <path d="M52 40h16" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      </template>
      <!-- 默认：资料夹 -->
      <template v-else>
        <path d="M24 30a6 6 0 016-6h18l8 10h24a6 6 0 016 6v34a6 6 0 01-6 6H30a6 6 0 01-6-6V30z" stroke="currentColor" stroke-width="2" />
        <path d="M40 54h40M40 66h26" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      </template>
    </svg>
    <p class="empty-title">{{ title || '暂无数据' }}</p>
    <p v-if="desc" class="empty-desc">{{ desc }}</p>
  </div>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    // todo | ledger | chart | student | folder
    type: { type: String, default: 'folder' },
    title: { type: String, default: '' },
    desc: { type: String, default: '' }
  }
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 34px 16px 30px;
  color: #c3cedb;
  user-select: none;
}
.empty-svg {
  width: 96px;
  height: 78px;
  margin-bottom: 14px;
  color: #c3cedb;
}
.empty-title {
  margin: 0;
  font-size: 13px;
  color: #8e9bad;
  font-weight: 500;
}
.empty-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: #a9b4c2;
  line-height: 1.6;
  text-align: center;
  max-width: 320px;
}
</style>
