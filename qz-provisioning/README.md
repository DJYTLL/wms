# QZ Tray Provisioning

这套目录用于解决 QZ Tray 在客户机上的“信任与授权”问题，目标是让浏览器发起的打印请求在安装完成后尽量无感执行，不再依赖用户手工点击 `Remember this decision`。

## 解决的问题

- 信任当前业务系统使用的自签打印证书
- 预写 `allowed.dat`，减少首次授权的人工操作
- 给客户提供一个可双击执行的安装入口

## 目录结构

- [windows/provision.json](D:/project/qz-provisioning/windows/provision.json)
  QZ 官方 provisioning 配置，包含 `ca` 和 `cert` 两类规则。
- [windows/self-signed.crt](D:/project/qz-provisioning/windows/self-signed.crt)
  QZ 需要信任的自签证书。
- [windows/digital-certificate.txt](D:/project/qz-provisioning/windows/digital-certificate.txt)
  写入 `allowed.dat` 的业务证书。
- [windows/install-qz-provision.ps1](D:/project/qz-provisioning/windows/install-qz-provision.ps1)
  实际执行安装的 PowerShell 脚本。
- [windows/launch-qz-provision.ps1](D:/project/qz-provisioning/windows/launch-qz-provision.ps1)
  负责目录选择、提权和停窗的 PowerShell 启动器。
- [windows/install-qz-provision.bat](D:/project/qz-provisioning/windows/install-qz-provision.bat)
  面向客户的双击入口。

## 安装前提

安装前请确认：

- Windows 已安装目标针式打印机驱动
- `QZ Tray` 已安装
- 客户有管理员权限，或现场人员能提供管理员授权
- 当前系统已经接入后端签名接口和前端 QZ 握手逻辑

## 客户安装步骤

推荐方式：

1. 打开 [windows/install-qz-provision.bat](D:/project/qz-provisioning/windows/install-qz-provision.bat)
2. 如果没有传安装目录参数，脚本会弹出文件夹选择框
3. 选择 `QZ Tray` 安装目录
4. 确认 UAC 管理员提权
5. 等待脚本执行完成
6. 按提示关闭窗口
7. 回到系统测试打印

如果你已经知道 `QZ Tray` 安装目录，也可以带路径运行：

```powershell
.\install-qz-provision.bat "D:\YourPath\QZ Tray"
```

## 脚本实际做了什么

安装脚本会依次执行这些动作：

1. 把 `provision.json`、`self-signed.crt`、`digital-certificate.txt` 复制到 `QZ Tray\provision`
2. 调用 `qz-tray-console.exe certgen`，触发 `certgen` 阶段的 provisioning
3. 调用 `qz-tray-console.exe --allow`，把业务证书写入当前用户的 `allowed.dat`
4. 重启 `QZ Tray`

## 验收方式

安装完成后，建议按下面顺序验证：

1. 打开系统登录页并登录
2. 进入打印模板页或任意打印页
3. 点击打印
4. 确认不再出现之前那种匿名未签名授权问题
5. 确认打印任务进入目标打印机，而不是 PDF 打印机

## 常见问题

### 1. 双击 `.bat` 没反应

先在 PowerShell 中进入当前目录后执行：

```powershell
.\install-qz-provision.bat
```

### 2. 找不到 `QZ Tray` 安装目录

可以在脚本弹出的文件夹选择框中手动选择，也可以直接传路径参数：

```powershell
.\install-qz-provision.bat "D:\YourPath\QZ Tray"
```

### 3. 打印时还是保存成 PDF

这通常不是信任问题，而是系统默认打印机仍然是 `Microsoft Print to PDF` 或其他虚拟打印机。需要把默认打印机切到实际针式打印机，或者后续在系统里配置固定打印机名称。

### 4. 更换了后端证书后又失效

如果后端的 `qz-dev.pfx` 或 `qz-dev-cert.pem` 发生变化，需要同步更新：

- [windows/self-signed.crt](D:/project/qz-provisioning/windows/self-signed.crt)
- [windows/digital-certificate.txt](D:/project/qz-provisioning/windows/digital-certificate.txt)

更新后，需要在客户机重新执行一次安装脚本。

### 5. 客户使用正式域名时仍提示浏览器限制

这通常不是 QZ 授权本身的问题，而是 Chrome / Edge 对本地网络访问的限制。正式环境可能还需要补充 Local Network Access 策略。

## 维护建议

正式交付时建议把这一目录单独打包给客户，并配一份简版现场说明。后续如果你们要切换正式证书或正式域名，这个目录里的证书文件需要一起更新，不要只改后端。
