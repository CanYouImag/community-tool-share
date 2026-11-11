# 小组作业贡献说明

1. Fork该仓库，从最新main创建分支
2. 修改或实现功能
3. 提交Pull Request接受审核

## 代码格式

在仓库根目录安装格式化工具并运行：

```powershell
npm.cmd ci
npm.cmd run format
npm.cmd run format:check
```

统一使用根目录的 `.prettierrc.json` 格式化。

提交前运行 `npm.cmd run format:check`；格式整理尽量单独提交，便于审核业务改动。

## 提交信息要求

这边不懂应该填什么提交信息的问AI

格式：

```text
<修改类型>(<涉及的模块或文件>): <修改内容>
```

允许的修改类型：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`、`revert`。

比方说：

```text
feat(frontend/classA): 实现A功能
fix(backecd/test): 修复test模块某某问题
docs(user-manual): 完善用户手册
style(frontend/index.html): 美化界面
等等，更多不懂的问AI
```
