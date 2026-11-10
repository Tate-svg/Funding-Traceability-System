// 只由真实的弹窗交互触发续期；计时以绝对截止时刻计算，避免后台标签页节流漏计。
export function createSensitiveIdleSession({
  idleSeconds, remainingSeconds, absoluteSecondsRemaining, renew, onExpire, onTick,
  now = Date.now, setTimer = setTimeout, clearTimer = clearTimeout, renewalIntervalMs = 30000,
  initialRequestAt
}) {
  let active = true
  // 首次加载同样扣除网络传输时间，不把慢响应当作刚开始的授权期限。
  let lastActivity = Number.isFinite(initialRequestAt) ? Math.min(initialRequestAt, now()) : now()
  let serverDeadline = lastActivity + remainingSeconds * 1000
  let absoluteDeadline = lastActivity + absoluteSecondsRemaining * 1000
  let nextRenewAt = lastActivity + renewalIntervalMs
  let timer = null
  let inFlight = false
  let activityVersion = 0

  function stop() {
    active = false
    if (timer !== null) clearTimer(timer)
    timer = null
  }
  function check() {
    if (!active) return false
    const deadline = Math.min(lastActivity + idleSeconds * 1000, serverDeadline, absoluteDeadline)
    const seconds = Math.max(0, Math.ceil((deadline - now()) / 1000))
    onTick(seconds)
    if (seconds === 0) {
      stop()
      onExpire()
      return false
    }
    return true
  }
  function scheduleRenewal() {
    if (!active || inFlight || timer !== null) return
    timer = setTimer(() => {
      timer = null
      if (!check()) return
      inFlight = true
      const startedAt = now()
      const version = activityVersion
      nextRenewAt = startedAt + renewalIntervalMs
      Promise.resolve().then(renew).then(data => {
        if (!active) return
        if (!data || !Number.isFinite(data.remainingSeconds) || !Number.isFinite(data.absoluteSecondsRemaining)) throw new Error('Invalid session deadline')
        // 用请求发起时刻扣除传输时间，避免客户端把服务端期限延长。
        serverDeadline = startedAt + data.remainingSeconds * 1000
        absoluteDeadline = Math.min(absoluteDeadline, startedAt + data.absoluteSecondsRemaining * 1000)
        if (check() && version !== activityVersion) scheduleAfterResponse = true
      }).catch(() => {
        if (active) { stop(); onExpire() }
      }).finally(() => {
        inFlight = false
        if (scheduleAfterResponse) { scheduleAfterResponse = false; scheduleRenewal() }
      })
    }, Math.max(0, nextRenewAt - now()))
  }
  let scheduleAfterResponse = false
  function activity() {
    if (!check()) return
    lastActivity = now()
    activityVersion++
    scheduleRenewal()
  }
  return { activity, check, stop }
}
