// EPIC-RPT types, mirrored 1:1 against TabularReport.java (read fresh, not assumed).

export interface TabularReport {
  key: string
  title: string
  generatedAt: string
  columns: string[]
  rows: string[][]
}
