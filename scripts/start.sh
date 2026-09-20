#!/usr/bin/env bash
# 辅助启动脚本（Linux/macOS）
set -e
echo "正在一键启动社区闲置工具共享与租赁平台..."
docker compose up -d
echo "启动完成：前端 http://localhost:5173 ，后端 http://localhost:8080"