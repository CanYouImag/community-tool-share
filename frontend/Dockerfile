# 邻租前端：Vite 构建 → Nginx 托管静态资源并代理 /api
FROM node:20-alpine AS build
WORKDIR /app
# 中国网络环境默认走 npmmirror；如需官方源：--build-arg NPM_REGISTRY=https://registry.npmjs.org
ARG NPM_REGISTRY=https://registry.npmmirror.com
COPY package.json package-lock.json ./
RUN npm install --registry=$NPM_REGISTRY --no-audit --no-fund
COPY . .
# 关闭 Mock，构建对接真实后端的产物
RUN VITE_USE_MOCK=false npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
