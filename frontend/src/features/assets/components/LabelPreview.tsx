import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import ButtonGroup from '@mui/material/ButtonGroup'
import CircularProgress from '@mui/material/CircularProgress'
import Typography from '@mui/material/Typography'
import { useLabelImage } from '../hooks/useLabelImage'

const FORMATS = ['png', 'svg', 'pdf'] as const

// Label rendering is deliberately independent of asset creation succeeding -
// it only reads the already-persisted barcode/QR values (AC-AST-02-X), so
// this preview can be re-fetched/retried indefinitely with no side effects.
export function LabelPreview({ assetId, assetNumber }: { assetId: string; assetNumber: string }) {
  const [format, setFormat] = useState<(typeof FORMATS)[number]>('png')
  const { url, error, loading } = useLabelImage(assetId, format)

  return (
    <Box>
      <ButtonGroup size="small" sx={{ mb: 1 }}>
        {FORMATS.map((f) => (
          <Button key={f} variant={f === format ? 'contained' : 'outlined'} onClick={() => setFormat(f)}>
            {f.toUpperCase()}
          </Button>
        ))}
      </ButtonGroup>

      <Box
        sx={{
          border: 1,
          borderColor: 'divider',
          borderRadius: 1,
          p: 2,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: 140,
        }}
      >
        {loading && <CircularProgress size={24} />}
        {!loading && Boolean(error) && (
          <Typography variant="body2" color="error">
            Could not load label preview.
          </Typography>
        )}
        {!loading && !error && url && format !== 'pdf' && (
          <img src={url} alt={`Label for ${assetNumber}`} style={{ maxWidth: '100%' }} />
        )}
        {!loading && !error && url && format === 'pdf' && (
          <Button href={url} download={`${assetNumber}-label.pdf`} variant="outlined">
            Download PDF
          </Button>
        )}
      </Box>
    </Box>
  )
}
