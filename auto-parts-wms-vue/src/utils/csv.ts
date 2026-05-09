type CsvColumn<T> = {
  key: keyof T | string
  label: string
  formatter?: (row: T) => string | number | null | undefined
}

const escapeCsv = (value: string) => {
  if (value.includes('"') || value.includes(',') || value.includes('\n') || value.includes('\r')) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

export const exportToCsv = <T extends Record<string, any>>(
  filename: string,
  columns: CsvColumn<T>[],
  rows: T[]
) => {
  const headers = columns.map((col) => escapeCsv(col.label))
  const lines = rows.map((row) => {
    return columns
      .map((col) => {
        const value = col.formatter ? col.formatter(row) : row[col.key as keyof T]
        if (value === null || value === undefined) return ''
        return escapeCsv(String(value))
      })
      .join(',')
  })
  const content = [headers.join(','), ...lines].join('\n')
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
