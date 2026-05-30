# Windows 本机 Node 环境记录

适用机器：当前 `D:\project` 所在这台 Windows 开发机。

## 当前有效环境

- Node 安装目录：`D:\tools\nodejs`
- `node -v`：`v24.16.0`
- `npm -v`：`11.13.0`
- `npx -v`：`11.13.0`

当前会话里，建议先把 `D:\tools\nodejs` 放到 `Path` 最前面再执行前端命令：

```powershell
$env:Path = 'D:\tools\nodejs;' + (($env:Path -split ';' | Where-Object { $_ -and $_ -ne 'D:\tools\nodejs' }) -join ';')
```

然后再运行：

```powershell
node -v
npm -v
npx -v
```

## 当前验证结果

在当前机器上执行 `where.exe` 时，命令解析结果包含：

- `node`：
  - `D:\tools\nodejs\node.exe`
  - `C:\nvm4w\nodejs\node.exe`
  - `C:\Program Files\WindowsApps\OpenAI.Codex_26.519.11010.0_x64__2p2nqsd0c76g0\app\resources\node.exe`
- `npm`：
  - `D:\tools\nodejs\npm`
  - `D:\tools\nodejs\npm.cmd`
  - `C:\nvm4w\nodejs\npm`
  - `C:\nvm4w\nodejs\npm.cmd`
- `npx`：
  - `D:\tools\nodejs\npx`
  - `D:\tools\nodejs\npx.cmd`
  - `C:\nvm4w\nodejs\npx`
  - `C:\nvm4w\nodejs\npx.cmd`

结论：

- 当前可用主环境是 `D:\tools\nodejs`
- 旧的 `nvm4w` 和 WindowsApps Node 痕迹仍然存在
- 如果不主动把 `D:\tools\nodejs` 顶到 `Path` 前面，后续可能再次混用旧环境

## 已确认的清理进度

已处理的用户级残留：

- 已移除 `HKCU\Environment\NVM_HOME`
- 已移除 `HKCU\Environment\NVM_SYMLINK`
- 已移除 `HKCU\Software\Microsoft\Command Processor\AutoRun`

未完全处理的部分：

- 机器级 `Path` 中旧 `nvm4w` 路径清理曾尝试执行，但当时因权限限制失败
- 因此当前仍建议在每次新终端会话里显式注入一次上面的 `Path` 命令

## 当前项目的推荐用法

前端目录：`D:\project\auto-parts-wms-vue`

推荐执行顺序：

```powershell
$env:Path = 'D:\tools\nodejs;' + (($env:Path -split ';' | Where-Object { $_ -and $_ -ne 'D:\tools\nodejs' }) -join ';')
Set-Location D:\project\auto-parts-wms-vue
npm run type-check
```

如果要跑 Node 原生测试，也先保留同一套 `Path` 前缀：

```powershell
$env:Path = 'D:\tools\nodejs;' + (($env:Path -split ';' | Where-Object { $_ -and $_ -ne 'D:\tools\nodejs' }) -join ';')
node --test D:\project\auto-parts-wms-vue\src\views\erp\__tests__\erpSupplierDialogRedesign.test.mjs
```

## 维护约定

- 后续在这台机器上执行前端命令，默认以 `D:\tools\nodejs` 为准
- 如果再做管理员级环境清理，需要优先移除旧 `C:\nvm4w\nodejs` 相关机器级 `Path`
- 在确认机器级 `Path` 已彻底修正前，不要把“where 里还能看到旧 Node”误判为当前会话实际在用旧 Node；先看 `node -v` 和 `where.exe node` 的首条结果
