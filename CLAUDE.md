# AI Image Manager

全栈 Web 应用，用于整理、浏览和管理 AI 生成的图片（Stable Diffusion 等）。支持自动元数据提取、文件夹树组织、彩色标签分类和批量操作。

## 技术栈

- **后端**: Java 21, Spring Boot 4.0.6, Spring Data JPA / Hibernate, MySQL 8+
- **前端**: Vue 3 + TypeScript, Vite 8, Vue Router 5, Pinia 3, Element Plus 2.13
- **图片处理**: Thumbnailator 0.4.20（缩略图）
- **API 文档**: SpringDoc OpenAPI 2.8.5（Swagger UI）
- **构建**: Maven（后端 `./mvnw`），npm（前端）

## 项目结构

- `backend/` — Spring Boot 后端（Maven 项目）
- `frontend/` — Vue 3 + TypeScript 前端（Vite 项目）
- `storage/` — 图片存储（gitignored），含 `originals/` 和 `thumbnails/`

完整目录结构和数据流详见 `README.md`。

## 开发命令

**后端：**
```bash
cd backend
./mvnw clean install -DskipTests   # 构建（跳过测试）
./mvnw spring-boot:run             # 启动（端口 8080）
./mvnw test                        # 运行测试
```

**前端：**
```bash
cd frontend
npm install      # 安装依赖
npm run dev      # 启动开发服务器（端口 5173）
npm run build    # 类型检查 + 生产构建
npm run preview  # 预览生产构建
```

## 关键约定

1. **前后端分离**：开发时同时启动 backend（:8080）和 frontend（:5173），前端通过 Vite proxy 转发 `/api/*` 和 `/files/*`
2. **数据库**：Hibernate ddl-auto=update，实体变更后自动同步表结构。MySQL 需提前启动（docker 或本地），详见 `README.md` 快速开始
3. **图片存储**：`storage/originals/` 存原图（UUID 重命名），`storage/thumbnails/` 存缩略图（最大 300px）。删除图片时同时清理两个目录
4. **元数据提取**：`MetadataExtractorService` 使用**原始二进制解析** PNG 的 tEXt/iTXt 块（替代 javax.imageio，避免大块截断问题）。支持 SDXL（KSampler）和 Flux.2（UltimateSDUpscale、UNETLoader）+ LoRA 工作流。完整 workflow JSON 和 prompt JSON 分别存入 `workflowJson`/`comfyuiPromptJson` 字段。A1111 格式（`parameters`/`Description`）也支持。全部为"尽力而为"策略，失败静默跳过
5. **CORS**：开发环境允许 `localhost:5173`，参见 `CorsConfig.java`
6. **标签操作语义**：`PUT /api/images/{id}/tags` = 整体替换；`POST /api/images/{id}/tags` = 追加；`DELETE /api/images/{id}/tags` = 移除指定标签。批量操作同理（`POST /api/images/batch/tags` 追加，`DELETE /api/images/batch/tags` 批量移除）。批量插入使用 `JdbcTemplate.batchUpdate` + `INSERT IGNORE` 以避免 N+1 性能问题
7. **图片下载**：`GET /api/images/{id}/download` 下载原图，`GET /api/images/{id}/workflow` 下载 workflow JSON。仅在 `workflowJson` 非空时前端显示下载按钮
8. **文件管理**：`FileController` 提供文件系统浏览能力——`GET /api/files/drives`（盘符）、`GET /api/files/browse?path=`（目录内容）、`GET /api/files/stats`（存储统计）。目录扫描最多递归 3 层，仅导入 `app.scan.allowed-extensions` 中配置的格式

## 已知问题 / 坑点

- 缩略图生成依赖 Thumbnailator，损坏的图片文件会导致上传失败
- `storage/` 目录被 gitignore，新环境需手动创建 `storage/originals/` 和 `storage/thumbnails/`
- 批量标签对话框只显示选中图片的**共同标签**（交集），部分图片独有的标签不会展示
- `batchTag` 使用原生 JDBC 批量插入绕过 JPA 缓存，如果在同一事务中后续读取被修改的图片会读到脏数据——当前设计下 `batchTag` 是独立事务，因此无此问题
