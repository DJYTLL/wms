import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const srcRoot = path.join(projectRoot, 'src')

const walkVueFiles = (dir) => fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
  const fullPath = path.join(dir, entry.name)
  if (entry.isDirectory()) {
    return walkVueFiles(fullPath)
  }
  return entry.isFile() && entry.name.endsWith('.vue') ? [fullPath] : []
})

const readAttr = (tag, names) => {
  for (const name of names) {
    const match = tag.match(new RegExp(`${name}\\s*=\\s*"([^"]+)"`))
    if (match) {
      return match[1]
    }
  }
  return ''
}

const inferColumnKey = (tag) => (
  readAttr(tag, ['column-key', 'columnKey'])
  || readAttr(tag, ['prop'])
  || readAttr(tag, ['type'])
)

const lineOf = (text, index) => text.slice(0, index).split(/\r?\n/).length
const toRelative = (file) => path.relative(projectRoot, file).replace(/\\/g, '/')

const issues = []

for (const file of walkVueFiles(srcRoot)) {
  const text = fs.readFileSync(file, 'utf8')
  const relativeFile = toRelative(file)
  const tablePattern = /<ErpDataTable\b[\s\S]*?<\/ErpDataTable>/g
  let tableMatch

  while ((tableMatch = tablePattern.exec(text))) {
    const block = tableMatch[0]
    const tableLine = lineOf(text, tableMatch.index)
    const tableStart = block.match(/<ErpDataTable\b[^>]*>/)?.[0] || ''
    const staticTableKey = readAttr(tableStart, ['table-key'])
    const dynamicTableKey = readAttr(tableStart, [':table-key'])
    const tableKey = staticTableKey || dynamicTableKey

    if (!tableKey) {
      issues.push(`${relativeFile}:${tableLine} ErpDataTable 缺少 table-key`)
    }

    if (staticTableKey && /-\d+$/.test(staticTableKey)) {
      issues.push(`${relativeFile}:${tableLine} table-key "${staticTableKey}" 仍是数字后缀命名`)
    }

    const columnKeys = []
    for (const columnMatch of block.matchAll(/<ErpDataTableColumn\b[^>]*>/g)) {
      const columnKey = inferColumnKey(columnMatch[0])
      if (columnKey) {
        columnKeys.push(columnKey)
      }
    }

    const duplicates = [...new Set(columnKeys.filter((key, index) => columnKeys.indexOf(key) !== index))]
    for (const duplicate of duplicates) {
      issues.push(`${relativeFile}:${tableLine} table-key "${tableKey || '(missing)'}" 存在重复列 key "${duplicate}"`)
    }

    const customKeys = [...new Set(columnKeys.filter((key) => /^custom-\d+$/.test(key)))]
    for (const customKey of customKeys) {
      issues.push(`${relativeFile}:${tableLine} table-key "${tableKey || '(missing)'}" 仍使用非语义列 key "${customKey}"`)
    }
  }

  for (const keyMatch of text.matchAll(/\bkey:\s*['"](custom-\d+)['"]/g)) {
    issues.push(`${relativeFile}:${lineOf(text, keyMatch.index)} columns 配置仍使用非语义 key "${keyMatch[1]}"`)
  }
}

if (issues.length > 0) {
  console.error(`表格 key 检查失败，共 ${issues.length} 个问题：`)
  for (const issue of issues) {
    console.error(`- ${issue}`)
  }
  process.exit(1)
}

console.log('表格 key 检查通过')
