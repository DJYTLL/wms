import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const filePath = resolve(scriptDir, '../src/views/erp/ErpVehicleFitmentManagement.vue')
const source = readFileSync(filePath, 'utf8')

const dialogTags = [...source.matchAll(/<el-dialog\b[^>]*>/g)].map((match) => match[0])
const missingAppendToBody = dialogTags.filter((tag) => !/\sappend-to-body(?:\s|>|=)/.test(tag))

if (dialogTags.length !== 4) {
  throw new Error(`Expected 4 vehicle fitment dialogs, found ${dialogTags.length}`)
}

if (missingAppendToBody.length > 0) {
  throw new Error(`Vehicle fitment dialogs must append to body: ${missingAppendToBody.join(' ')}`)
}
