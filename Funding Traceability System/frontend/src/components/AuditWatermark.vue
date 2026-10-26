<template>
  <!-- 审计专用视图水印：仅挂在审计页面，路由切换离开即销毁 -->
  <div class="audit-watermark" aria-hidden="true">
    <div class="wm-tiles" :style="tileStyle"></div>
  </div>
</template>

<script>
/**
 * 审计页面实时水印
 * 内容固定为「当前登录账号 + 访问时间」，平铺半透明，不拦截任何鼠标操作；
 * 账号变化时自动重绘，组件随审计页面一同销毁，切回普通业务视图水印立即消失。
 */
export default {
  name: 'AuditWatermark',
  data() {
    return {
      accessTime: '',
      tile: ''
    }
  },
  computed: {
    username() {
      return this.$store.getters.username || '未登录'
    },
    tileStyle() {
      return { backgroundImage: this.tile ? 'url("' + this.tile + '")' : 'none' }
    }
  },
  watch: {
    username() {
      this.renderTile()
    }
  },
  created() {
    this.accessTime = this.formatNow()
    this.renderTile()
  },
  methods: {
    formatNow() {
      const d = new Date()
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
        ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
    },
    renderTile() {
      const width = 320
      const height = 200
      const dpr = window.devicePixelRatio || 1
      const canvas = document.createElement('canvas')
      canvas.width = width * dpr
      canvas.height = height * dpr
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      ctx.scale(dpr, dpr)
      ctx.translate(width / 2, height / 2)
      ctx.rotate(-Math.PI / 8)
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      // 低透明度浅蓝，保证单据核心文字可读
      ctx.fillStyle = 'rgba(43, 108, 176, 0.13)'
      ctx.font = '600 14px "Microsoft YaHei", "PingFang SC", "SimHei", sans-serif'
      ctx.fillText(this.username, 0, -9)
      ctx.font = '12px "Microsoft YaHei", "PingFang SC", "SimHei", sans-serif'
      ctx.fillText(this.accessTime, 0, 11)
      this.tile = canvas.toDataURL('image/png')
    }
  }
}
</script>

<style scoped>
.audit-watermark {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1800;
  overflow: hidden;
  pointer-events: none;
  user-select: none;
}
.wm-tiles {
  width: 100%;
  height: 100%;
  background-repeat: repeat;
  background-size: 320px 200px;
}
</style>
