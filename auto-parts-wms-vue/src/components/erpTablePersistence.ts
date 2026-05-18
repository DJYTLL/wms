import type { ComputedRef, InjectionKey } from 'vue'

export type PersistedElementTableColumn = {
  key: string
  label: string
  configurable?: boolean
  defaultFixed?: 'left' | 'right'
}

export type PersistedElementTableContext = {
  tableKey: ComputedRef<string>
  registerColumn: (column: PersistedElementTableColumn) => () => void
  resolveSavedWidth: (columnKey: string) => number | undefined
  resolveFixed: (columnKey: string, fallback?: '' | 'left' | 'right') => '' | 'left' | 'right'
  isColumnVisible: (columnKey: string, configurable?: boolean) => boolean
  saveWidth: (columnKey: string, width: number) => Promise<void>
}

export const persistedElementTableKey: InjectionKey<PersistedElementTableContext> = Symbol('persistedElementTable')
