<template>
  <div class="line-chart">
    <svg
      :viewBox="`0 0 ${W} ${H}`"
      style="display:block; width:100%; height:auto"
      @mousemove="onMove"
      @mouseleave="hover = null"
    >
      <!-- 横向网格线 + Y 轴刻度 -->
      <g>
        <line
          v-for="(g, i) in gridLines"
          :key="'g' + i"
          :x1="padL"
          :x2="W - padR"
          :y1="g.y"
          :y2="g.y"
          stroke="#ebeef5"
          stroke-width="1"
        />
        <text
          v-for="(g, i) in gridLines"
          :key="'gl' + i"
          :x="padL - 8"
          :y="g.y + 4"
          text-anchor="end"
          font-size="11"
          fill="#909399"
        >{{ formatY(g.value) }}</text>
      </g>

      <!-- X 轴标签（MM-DD） -->
      <text
        v-for="(lb, i) in xLabels"
        :key="'x' + i"
        :x="lb.x"
        :y="H - padB + 18"
        text-anchor="middle"
        font-size="11"
        fill="#909399"
      >{{ lb.text }}</text>

      <!-- 面积填充 -->
      <path v-if="area && pathArea" :d="pathArea" :fill="color" fill-opacity="0.14" />

      <!-- 折线 -->
      <path v-if="pathLine" :d="pathLine" fill="none" :stroke="color" stroke-width="2" />

      <!-- 数据点 -->
      <circle
        v-for="(p, i) in pts"
        :key="'p' + i"
        :cx="p.x"
        :cy="p.y"
        r="2.5"
        :fill="color"
      />

      <!-- hover 参考线 + 高亮点 -->
      <line
        v-if="hover"
        :x1="hover.x"
        :x2="hover.x"
        :y1="padT"
        :y2="H - padB"
        stroke="#c0c4cc"
        stroke-width="1"
        stroke-dasharray="3 3"
      />
      <circle
        v-if="hover"
        :cx="hover.x"
        :cy="hover.y"
        r="4"
        :fill="color"
        stroke="#fff"
        stroke-width="2"
      />
    </svg>

    <div v-if="hover" class="lc-tooltip" :style="tooltipStyle">
      <div class="lc-date">{{ hover.label }}</div>
      <div class="lc-val">{{ formatValue(hover.value) }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  // 数据点：[{ label: 'yyyy-MM-dd', value: Number }]
  points: { type: Array, default: () => [] },
  color: { type: String, default: '#409eff' },
  area: { type: Boolean, default: false },
  height: { type: Number, default: 260 },
  // 'zero' 从 0 起；'auto' 从最小值附近起（凸显增长趋势）
  yMinMode: { type: String, default: 'zero' },
  // 自定义数值格式化（如字节 -> MB/GB）
  valueFormatter: { type: Function, default: null }
})

const W = 800
const H = computed(() => props.height)
const padL = 56
const padR = 16
const padT = 16
const padB = 36

const values = computed(() => props.points.map(p => Number(p.value) || 0))
// 从 0 起时，把最大值向上取整到 4 的整数倍，避免 Y 轴刻度出现 5/9/14/18 这种不整齐值
const maxV = computed(() => {
  const raw = Math.max(1, ...values.value)
  if (props.yMinMode === 'zero') {
    const tick = Math.max(1, Math.ceil(raw / 4))
    return tick * 4
  }
  return raw
})
const minV = computed(() => {
  if (props.yMinMode === 'auto') {
    const m = Math.min(...values.value)
    return m > 0 ? m * 0.92 : 0
  }
  return 0
})

const plotW = computed(() => W - padL - padR)
const plotH = computed(() => H.value - padT - padB)

function xAt(i) {
  const n = props.points.length
  if (n <= 1) return padL + plotW.value / 2
  return padL + (plotW.value * i) / (n - 1)
}
function yAt(v) {
  const span = (maxV.value - minV.value) || 1
  return padT + plotH.value * (1 - (v - minV.value) / span)
}

const pts = computed(() =>
  props.points.map((p, i) => ({
    x: xAt(i),
    y: yAt(Number(p.value) || 0),
    label: p.label,
    value: Number(p.value) || 0
  }))
)

const pathLine = computed(() => {
  if (!pts.value.length) return ''
  return pts.value
    .map((p, i) => (i === 0 ? 'M' : 'L') + p.x.toFixed(1) + ' ' + p.y.toFixed(1))
    .join(' ')
})

const pathArea = computed(() => {
  if (!pts.value.length || !props.area) return ''
  const top = pts.value
    .map((p, i) => (i === 0 ? 'M' : 'L') + p.x.toFixed(1) + ' ' + p.y.toFixed(1))
    .join(' ')
  const last = pts.value[pts.value.length - 1]
  const first = pts.value[0]
  const base = `L${last.x.toFixed(1)} ${H.value - padB} L${first.x.toFixed(1)} ${H.value - padB} Z`
  return top + ' ' + base
})

const gridLines = computed(() => {
  const n = 4
  const arr = []
  for (let i = 0; i <= n; i++) {
    const v = minV.value + (maxV.value - minV.value) * (i / n)
    arr.push({ y: yAt(v), value: v })
  }
  return arr
})

const xLabels = computed(() => {
  const n = props.points.length
  if (!n) return []
  const step = Math.max(1, Math.ceil(n / 6))
  const arr = []
  for (let i = 0; i < n; i += step) {
    arr.push({ x: xAt(i), text: props.points[i].label.slice(5) })
  }
  if ((n - 1) % step !== 0) {
    arr.push({ x: xAt(n - 1), text: props.points[n - 1].label.slice(5) })
  }
  return arr
})

function formatValue(v) {
  if (props.valueFormatter) return props.valueFormatter(v)
  return String(v)
}
function formatY(v) {
  return formatValue(Math.round(v))
}

const hover = ref(null)
function onMove(e) {
  const svg = e.currentTarget
  const rect = svg.getBoundingClientRect()
  const xRatio = (e.clientX - rect.left) / rect.width
  const xPx = xRatio * W
  let best = null
  let bestD = Infinity
  for (const p of pts.value) {
    const d = Math.abs(p.x - xPx)
    if (d < bestD) {
      bestD = d
      best = p
    }
  }
  hover.value = best
}

const tooltipStyle = computed(() => {
  if (!hover.value) return {}
  return {
    left: (hover.value.x / W) * 100 + '%',
    top: (hover.value.y / H.value) * 100 + '%'
  }
})
</script>

<style scoped>
.line-chart {
  position: relative;
}
.lc-tooltip {
  position: absolute;
  transform: translate(-50%, -120%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  pointer-events: none;
  white-space: nowrap;
  z-index: 5;
}
.lc-date {
  opacity: 0.85;
}
.lc-val {
  font-weight: 600;
}
</style>
