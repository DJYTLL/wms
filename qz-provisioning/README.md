# QZ Tray Provisioning

这套文件用于给 Windows 客户端侧载 QZ Tray 信任配置，目标是解决两件事：

- 信任当前业务系统使用的自签打印证书
- 预写 `allowed.dat`，避免用户手工点击 `Remember this decision`

采用的是 QZ 官方支持的两种机制：

- `ca` provisioning：让 QZ 信任自签证书
- `cert` provisioning：把业务证书加入允许列表

目录说明：

- `windows/provision.json`：QZ 官方 provisioning 配置
- `windows/self-signed.crt`：QZ 需要信任的自签证书
- `windows/digital-certificate.txt`：写入 `allowed.dat` 的业务证书
- `windows/install-qz-provision.ps1`：Windows 一键安装脚本
- `windows/launch-qz-provision.ps1`：BAT 调用的提权启动器

## 使用方式

1. 先安装 `QZ Tray`
2. 推荐直接右键“以管理员身份运行” `windows/install-qz-provision.bat`
3. 或者以管理员身份运行 `windows/install-qz-provision.ps1`
4. 脚本会把 provisioning 文件复制到 `C:\Program Files\QZ Tray\provision`
5. 脚本会调用 `qz-tray-console.exe certgen` 触发 `certgen` 阶段
6. 脚本会调用 `qz-tray-console.exe --allow` 预写当前用户的 `allowed.dat`
7. 脚本会重启 `QZ Tray`

### BAT 方式

双击或右键“以管理员身份运行”：

- `windows/install-qz-provision.bat`

如果没有传参数，脚本会先弹出文件夹选择框，让客户选择 `QZ Tray` 安装目录。

如果你已经知道安装目录，也可以直接附带路径参数：

- `windows/install-qz-provision.bat "D:\YourPath\QZ Tray"`

## 注意

- 当前文件面向 `localhost`/当前开发证书，只适合你们现有这一套签名接入
- 如果后端更换了 `qz-dev-cert.pem` 或 `qz-dev.pfx`，这里的两个证书文件也要同步更新
- 如果客户通过正式域名访问系统，Chrome/Edge 还可能需要额外配置 Local Network Access 策略
