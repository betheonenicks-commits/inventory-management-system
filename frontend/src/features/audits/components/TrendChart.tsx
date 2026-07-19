import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useTheme } from '@mui/material/styles'

export interface TrendSeries {
  label: string
  color: string
  values: (number | null)[]
}

interface TrendChartProps {
  title: string
  categories: string[]
  series: TrendSeries[]
  unit?: string
  height?: number
}

/**
 * US-AUD-18: a small, dependency-free inline-SVG line chart for a cross-cycle trend.
 * One numeric axis only (per the dataviz guidance - two measures of different scale
 * get two charts, never a dual axis); a legend appears for >=2 series so identity is
 * never colour-alone, and each point carries a native <title> tooltip. Theme-aware
 * through the MUI palette (recessive grid/axes, thin 2px lines).
 */
export function TrendChart({ title, categories, series, unit = '', height = 200 }: TrendChartProps) {
  const theme = useTheme()
  const gridColor = theme.palette.divider
  const axisText = theme.palette.text.secondary
  const surface = theme.palette.background.paper

  const W = 720
  const H = height
  const padL = 46
  const padR = 16
  const padT = 12
  const padB = 30
  const plotW = W - padL - padR
  const plotH = H - padT - padB

  const allVals = series.flatMap((s) => s.values).filter((v): v is number => v != null)
  const yMax = niceCeil(allVals.length ? Math.max(...allVals) : 1)
  const n = categories.length

  const xAt = (i: number) => (n <= 1 ? padL + plotW / 2 : padL + (i / (n - 1)) * plotW)
  const yAt = (v: number) => padT + plotH - (v / yMax) * plotH

  const tickCount = 4
  const yTicks = Array.from({ length: tickCount + 1 }, (_, i) => (yMax / tickCount) * i)

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline', mb: 0.5 }}>
        <Typography variant="subtitle2">{title}</Typography>
        {series.length > 1 && (
          <Stack direction="row" spacing={1.5}>
            {series.map((s) => (
              <Stack key={s.label} direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                <Box sx={{ width: 10, height: 10, borderRadius: '2px', bgcolor: s.color }} />
                <Typography variant="caption" color="text.secondary">
                  {s.label}
                </Typography>
              </Stack>
            ))}
          </Stack>
        )}
      </Stack>
      <Box
        component="svg"
        viewBox={`0 0 ${W} ${H}`}
        role="img"
        aria-label={title}
        sx={{ width: '100%', height: 'auto', display: 'block' }}
      >
        {yTicks.map((t, i) => (
          <g key={`y${i}`}>
            <line x1={padL} x2={W - padR} y1={yAt(t)} y2={yAt(t)} stroke={gridColor} strokeWidth={1} />
            <text x={padL - 6} y={yAt(t) + 3} textAnchor="end" fontSize={10} fill={axisText}>
              {round1(t)}
              {unit}
            </text>
          </g>
        ))}
        {categories.map((c, i) => (
          <text key={`x${i}`} x={xAt(i)} y={H - 10} textAnchor="middle" fontSize={10} fill={axisText}>
            {shorten(c)}
          </text>
        ))}
        {series.map((s) => {
          const points = s.values.map((v, i) => (v == null ? null : ([xAt(i), yAt(v)] as const)))
          const path = buildPath(points)
          return (
            <g key={s.label}>
              {path && (
                <path
                  d={path}
                  fill="none"
                  stroke={s.color}
                  strokeWidth={2}
                  strokeLinejoin="round"
                  strokeLinecap="round"
                />
              )}
              {points.map((p, i) =>
                p ? (
                  <circle key={i} cx={p[0]} cy={p[1]} r={3.5} fill={s.color} stroke={surface} strokeWidth={1.5}>
                    <title>{`${categories[i]} · ${s.label}: ${round1(s.values[i] as number)}${unit}`}</title>
                  </circle>
                ) : null,
              )}
            </g>
          )
        })}
      </Box>
    </Box>
  )
}

function buildPath(points: (readonly [number, number] | null)[]): string | null {
  let d = ''
  let penDown = false
  for (const p of points) {
    if (!p) {
      penDown = false
      continue
    }
    d += `${penDown ? 'L' : 'M'}${p[0].toFixed(1)} ${p[1].toFixed(1)} `
    penDown = true
  }
  return d.trim() || null
}

function niceCeil(v: number): number {
  if (v <= 0) return 1
  const mag = Math.pow(10, Math.floor(Math.log10(v)))
  const norm = v / mag
  const step = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10
  return step * mag
}

function round1(v: number): number {
  return Math.round(v * 10) / 10
}

function shorten(s: string): string {
  return s.length > 12 ? `${s.slice(0, 11)}…` : s
}
