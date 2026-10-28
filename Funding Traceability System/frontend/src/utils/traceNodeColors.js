/**
 * 溯源节点色卡（视觉层唯一来源）
 *
 * 说明：
 * 1. 这些色值同时用作实心底色承载白字（`.ts-badge` / `.node-tag` / 时间轴节点圆点），
 *    因此统一取深一档，保证白字对比度 ≥ 4.5:1（ui-audit-checklist §1.1）。
 * 2. 色相限定在蓝 / 绿 / 橙 / 青 / 蓝灰 系内，不使用紫色等越界色相。
 * 3. 仅影响展示，不参与任何业务判断。
 */
export const NODE_COLORS = {
  SUBMIT: '#2b6cb0',        // 提交申请 蓝
  AUDIT_PASS: '#1f7d52',    // 审核通过 绿
  AUDIT_REJECT: '#a83a36',  // 审核驳回 红
  REVIEW: '#a55e0e',        // 复核确认 橙
  REVIEW_DRAW: '#7b4a12',   // 大额抽检 深棕橙
  DISBURSE: '#8a6b10',      // 资金拨付 金
  VOUCHER_MODIFY: '#64748b',// 凭证修改 蓝灰
  VISIT_RECORD: '#257785',  // 回访录入 青
  STUDENT_UPLOAD: '#187a70',// 资料上传 蓝绿
  OTHER: '#66748a'          // 其他 灰
}

export function nodeColor(node) {
  return NODE_COLORS[node] || NODE_COLORS.OTHER
}

export default NODE_COLORS
