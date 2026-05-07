# AI Image Manager

全栈 Web 应用，用于整理、浏览和管理 AI 生成的图片（Stable Diffusion、Midjourney、DALL-E 等）。支持自动从 PNG 文件中提取生成元数据、文件夹树组织、彩色标签分类和批量操作。

## 技术栈

- **后端**: Java 21, Spring Boot 4.0.6, Spring Data JPA / Hibernate 7.2, MySQL 8+
- **前端**: Vue 3 + TypeScript, Vite 8, Vue Router 5, Pinia 3, Element Plus 2.13
- **图片处理**: Thumbnailator 0.4.20
- **API 文档**: SpringDoc OpenAPI 2.8.5 (Swagger UI)
- **构建**: Maven (backend), npm (frontend)

## 项目结构

- `backend/` — Spring Boot 后端 (REST API, 端口 8080)
- `frontend/` — Vue 3 + TypeScript 前端 (Vite 开发服务器, 端口 5173)
- `storage/` — 图片存储目录 (git 忽略, 含 `originals/` 和 `thumbnails/`)

## 开发命令

**后端** (在 `backend/` 下执行):
- `./mvnw spring-boot:run` — 启动开发服务器
- `./mvnw test` — 运行测试
- `./mvnw clean install -DskipTests` — 构建

**前端** (在 `frontend/` 下执行):
- `npm run dev` — 启动 Vite 开发服务器
- `npm run build` — 类型检查 + 生产构建

## 关键约定

- **DTO 使用 Java Record**: 所有 API 请求/响应 DTO 在 `backend/src/main/java/com/aiimage/dto/` 中定义为 Record
- **错误处理**: 后端统一通过 `GlobalExceptionHandler` 处理, 返回 `{ error, message, timestamp }` 格式
- **元数据提取**: `MetadataExtractorService` 直接解析 PNG 二进制 tEXt/iTXt 块 (替代 javax.imageio, 避免大块截断)。支持 A1111 格式和 ComfyUI JSON 格式 (SDXL / Flux.2)
- **批量标签**: 使用 `JdbcTemplate.batchUpdate` + `INSERT IGNORE` 一次性批量插入, 避免 N+1
- **前端代理**: Vite 代理 `/api/*` 和 `/files/*` 到 `localhost:8080`
- **状态管理**: 三个 Pinia Store — `imageStore` (图片列表/筛选/选中), `tagStore` (标签 CRUD), `folderStore` (文件夹树)

## 已知问题 / 坑点

- 需要 MySQL 8+ 运行中, 默认连接 `localhost:3306/ai_image_manager`
- 目录扫描最多递归 3 层, 仅导入 `app.scan.allowed-extensions` 中配置的格式
- `storage/` 目录被 git 忽略, 新环境需手动创建或由后端启动时自动创建
- Thumbnailator 可能在处理损坏图片时失败, 会导致整个上传失败
- 批量标签对话框只显示选中图片的**共同标签** (交集), 单张图片独有的标签不展示
- 首次启动后端需 Hibernate `ddl-auto: update` 自动建表, 需确保 MySQL 用户有 CREATE/ALTER 权限
