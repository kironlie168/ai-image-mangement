# AI Image Manager

全栈 Web 应用，用于整理、浏览和管理 AI 生成的图片（Stable Diffusion、Midjourney、DALL-E 等）。支持自动从 PNG 文件中提取生成元数据、文件夹树组织、彩色标签分类和批量操作。

## 主要功能

- **图片画廊** — 网格视图、分页排序、多选操作
- **搜索与筛选** — 按提示词、模型、文件名搜索；按文件夹、收藏、标签筛选
- **AI 元数据提取** — 自动从 PNG tEXt 块中提取提示词、反向提示词、模型、采样器、CFG scale、种子、尺寸等信息（支持 A1111 / ComfyUI 格式）
- **文件夹树** — 嵌套文件夹组织，支持图片计数
- **彩色标签** — 创建和管理彩色标签，支持批量分配
- **批量操作** — 多选图片进行批量标签、批量移除标签、批量删除
- **上传与扫描** — 上传单张图片并附加元数据，或扫描本地目录批量导入
- **文件系统浏览器** — 在浏览器中浏览本地磁盘和目录，选择扫描路径
- **存储统计** — 查看磁盘使用量、图片数量和缩略图数量
- **Swagger API 文档** — 完整的 OpenAPI 文档，访问 `/swagger-ui.html`
- **收藏系统** — 标记收藏图片并按收藏筛选

## 技术栈

- **后端**：Java 21, Spring Boot 4.0.6, Spring Data JPA / Hibernate 7.2, MySQL 8+
- **前端**：Vue 3 + TypeScript, Vite 8, Vue Router 5, Pinia 3, Element Plus 2.13
- **图片处理**：Thumbnailator 0.4.20（缩略图生成）
- **API 文档**：SpringDoc OpenAPI 2.8.5（Swagger UI）
- **构建工具**：Maven（后端），npm（前端）

---

## 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
  - [1. 克隆仓库](#1-克隆仓库)
  - [2. 数据库配置](#2-数据库配置)
  - [3. 后端配置](#3-后端配置)
  - [4. 前端配置](#4-前端配置)
  - [5. 启动开发服务器](#5-启动开发服务器)
- [架构说明](#架构说明)
  - [目录结构](#目录结构)
  - [数据流](#数据流)
  - [API 端点一览](#api-端点一览)
  - [数据库表结构](#数据库表结构)
- [配置说明](#配置说明)
  - [后端配置](#后端配置)
  - [前端配置](#前端配置)
- [可用命令](#可用命令)
- [API 参考](#api-参考)
  - [图片接口](#图片接口)
  - [文件夹接口](#文件夹接口)
  - [标签接口](#标签接口)
  - [文件系统接口](#文件系统接口)
- [项目结构详解](#项目结构详解)
- [部署指南](#部署指南)
  - [Docker 部署](#docker-部署)
  - [手动部署](#手动部署)
- [常见问题排查](#常见问题排查)

---

## 环境要求

- **Java 21** 或更高版本（推荐 OpenJDK）
- **Node.js 20+** 和 **npm 10+**
- **MySQL 8.0+**（本地运行或通过 Docker）
- 至少 **1 GB 内存**（供 Maven + Spring Boot 使用）
- 用于存储图片的本地目录（默认：`D:/projects/ai-image-manager/storage`）

---

## 快速开始

### 1. 克隆仓库

```bash
git clone <repository-url>
cd ai-image-manager
```

### 2. 数据库配置

使用 Docker 启动 MySQL（推荐）：

```bash
docker run --name mysql-image-manager \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=ai_image_manager \
  -p 3306:3306 \
  -d mysql:8.0
```

或确保本地 MySQL 已运行并创建数据库：

```bash
mysql -u root -p
CREATE DATABASE IF NOT EXISTS ai_image_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

数据库配置位于 `backend/src/main/resources/application.yml`：

| 配置项             | 默认值                                                           |
| ------------------ | ---------------------------------------------------------------- |
| 连接地址           | `jdbc:mysql://localhost:3306/ai_image_manager?useSSL=false...`   |
| 用户名             | `root`                                                           |
| 密码               | `root123`                                                        |
| 驱动               | `com.mysql.cj.jdbc.Driver`                                       |

根据你的 MySQL 配置调整以上值。

### 3. 后端配置

```bash
cd backend

# 构建项目（首次构建跳过测试）
./mvnw clean install -DskipTests

# Hibernate 会自动通过 ddl-auto=update 创建表结构
```

存储目录（`storage/originals/` 和 `storage/thumbnails/`）会在启动时自动创建，无需额外配置。

### 4. 前端配置

```bash
cd frontend
npm install
```

### 5. 启动开发服务器

**终端 1 — 后端（Spring Boot，端口 8080）：**

```bash
cd backend
./mvnw spring-boot:run
```

首次启动可能需要 30–60 秒（Maven 下载依赖 + Hibernate 初始化数据库）。看到以下日志即为启动成功：

```
Started AiImageManagerApplication in 5.218 seconds
```

**终端 2 — 前端（Vite 开发服务器，端口 5173）：**

```bash
cd frontend
npm run dev
```

看到以下输出即为启动成功：

```
VITE v8.0.10  ready in 561 ms
  ➜  Local:   http://localhost:5173/
```

**打开浏览器** 访问 [http://localhost:5173](http://localhost:5173)。

**Swagger API 文档：** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 架构说明

### 目录结构

```
ai-image-manager/
├── backend/                          # Spring Boot 后端应用
│   ├── src/main/java/com/aiimage/
│   │   ├── AiImageManagerApplication.java    # 应用入口
│   │   ├── config/
│   │   │   ├── CorsConfig.java       # CORS 跨域配置（允许 localhost:5173）
│   │   │   ├── WebConfig.java        # 静态资源映射 /files/**
│   │   │   └── SwaggerConfig.java    # OpenAPI 文档配置
│   │   ├── controller/
│   │   │   ├── ImageController.java  # /api/images — 图片 CRUD、搜索、上传、扫描、批量操作
│   │   │   ├── FolderController.java # /api/folders — 文件夹树 CRUD
│   │   │   ├── TagController.java    # /api/tags — 标签 CRUD
│   │   │   └── FileController.java   # /api/files — 文件系统浏览、存储统计
│   │   ├── service/
│   │   │   ├── ImageService.java     # 图片业务逻辑，批量标签使用 JDBC batchUpdate
│   │   │   ├── FolderService.java    # 文件夹树构建，嵌套 DTO 转换
│   │   │   ├── TagService.java       # 标签 CRUD，默认标签种子数据（10 个）
│   │   │   ├── FileStorageService.java  # 文件 I/O、缩略图生成、目录扫描
│   │   │   └── MetadataExtractorService.java  # PNG tEXt 元数据解析（A1111 / ComfyUI）
│   │   ├── entity/
│   │   │   ├── AiImage.java          # 图片 JPA 实体，含元数据字段
│   │   │   ├── Folder.java           # 层级文件夹实体（自引用）
│   │   │   └── Tag.java              # 彩色标签实体
│   │   ├── repository/
│   │   │   ├── AiImageRepository.java   # 自定义 @Query 搜索（关键词/文件夹/收藏/标签综合筛选）
│   │   │   ├── FolderRepository.java    # 树形查询（findByParentIdIsNull）
│   │   │   └── TagRepository.java       # 标签搜索，原生删除并清理图片关联
│   │   ├── dto/                      # Java Record 用于 API 请求/响应
│   │   │   ├── ImageDto.java         # 图片响应（含标签和文件夹信息）
│   │   │   ├── FolderDto.java        # 文件夹响应（含嵌套子文件夹和图片数）
│   │   │   ├── TagDto.java           # 标签响应（id, name, color）
│   │   │   ├── PageResponse.java     # 通用分页响应包装
│   │   │   ├── ImageUploadResponse.java  # 上传结果（每张文件 success/error）
│   │   │   ├── ScanRequest.java      # 目录扫描请求体
│   │   │   ├── BatchTagRequest.java  # 批量标签请求（@NotEmpty 校验）
│   │   │   ├── BatchRemoveTagRequest.java
│   │   │   ├── CreateTagRequest.java # 创建标签请求（@NotBlank 校验）
│   │   │   ├── UpdateTagRequest.java
│   │   │   └── UpdateMetadataRequest.java  # 元数据更新字段
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java  # 统一 REST 异常处理
│   │       ├── ResourceNotFoundException.java
│   │       ├── InvalidFileException.java
│   │       └── StorageException.java
│   ├── src/main/resources/
│   │   └── application.yml           # 数据库、存储、上传配置
│   └── pom.xml                       # Maven 依赖管理
│
├── frontend/                         # Vue 3 + TypeScript 前端应用
│   ├── src/
│   │   ├── main.ts                   # 应用启动：Pinia、Router、Element Plus
│   │   ├── App.vue                   # 根布局：导航栏、侧边栏、router-view
│   │   ├── router/index.ts           # 4 个路由：gallery、image-detail、upload、tags
│   │   ├── types/image.ts            # TypeScript 接口：ImageDto、TagDto、FolderDto、PageResponse
│   │   ├── api/                      # Axios HTTP 客户端
│   │   │   ├── http.ts               # Axios 实例（baseURL /api，错误拦截器）
│   │   │   ├── imageApi.ts           # 图片接口：上传、扫描、CRUD、标签、批量操作
│   │   │   ├── tagApi.ts             # 标签 CRUD 接口
│   │   │   ├── folderApi.ts          # 文件夹 CRUD + 树形接口
│   │   │   └── fileApi.ts            # 文件系统浏览、磁盘、存储统计
│   │   ├── stores/                   # Pinia 状态管理
│   │   │   ├── imageStore.ts         # 图片列表/详情、筛选、分页、选中、批量操作
│   │   │   ├── tagStore.ts           # 标签列表、CRUD
│   │   │   └── folderStore.ts        # 文件夹树、CRUD
│   │   ├── views/                    # 页面组件
│   │   │   ├── GalleryView.vue       # 图片画廊（搜索、排序、分页、批量操作栏）
│   │   │   ├── ImageDetailView.vue   # 图片详情（元数据查看/编辑、标签管理）
│   │   │   ├── UploadView.vue        # 上传页面（文件上传、目录扫描、存储统计）
│   │   │   └── TagManageView.vue     # 标签管理（CRUD、颜色选择器、搜索）
│   │   └── components/               # 可复用组件
│   │       ├── AppNavbar.vue         # 顶部导航（Gallery、Upload、Tags）
│   │       ├── AppSidebar.vue        # 侧边栏（文件夹树、收藏筛选、标签筛选）
│   │       ├── ImageGrid.vue         # 响应式图片网格（加载骨架屏 + 空状态）
│   │       ├── ImageCard.vue         # 图片卡片（缩略图、文件名、尺寸、标签、操作）
│   │       ├── SearchBar.vue         # 防抖搜索输入框 + 清除筛选按钮
│   │       ├── BatchActions.vue      # 批量操作栏（标签对话框、删除确认）
│   │       ├── TagSelector.vue       # 多选标签下拉（用于批量操作和详情页）
│   │       ├── FolderTree.vue        # el-tree 封装，用于文件夹选择
│   │       └── DirectoryPicker.vue   # 文件系统浏览器对话框（磁盘 → 目录）
│   ├── index.html                    # Vite 入口 HTML
│   ├── vite.config.ts                # Vite 配置（/api 和 /files 代理到 :8080）
│   └── package.json                  # 前端依赖
│
└── storage/                          # 图片存储目录（git 忽略）
    ├── originals/                    # 原始图片（UUID 重命名）
    └── thumbnails/                   # 缩略图（最大 300px，自动生成）
```

### 数据流

```
用户操作 → Vue 组件 → Pinia Store → Axios API 调用 → Vite 代理 (/api → :8080)
                                                        ↓
浏览器  ←  Vue Router  ←  Pinia 状态更新  ←  Spring Boot REST 控制器
                                                ↓
                                          Service 业务层
                                                ↓
                                        JPA Repository → MySQL
                                                ↓
                                        FileStorageService → 磁盘
```

### API 端点一览

| 方法   | 端点                             | 说明                                   |
| ------ | -------------------------------- | -------------------------------------- |
| GET    | `/api/images`                    | 搜索图片（关键词、文件夹、收藏、标签、分页、排序） |
| GET    | `/api/images/{id}`               | 获取单张图片详情                       |
| POST   | `/api/images/upload`             | 上传图片（multipart，每张最大 100MB）  |
| POST   | `/api/images/scan`               | 扫描本地目录并导入图片                 |
| DELETE | `/api/images/{id}`               | 删除单张图片 + 文件                    |
| DELETE | `/api/images/batch`              | 批量删除图片                           |
| PUT    | `/api/images/{id}/favorite`      | 切换收藏状态                           |
| PUT    | `/api/images/{id}/metadata`      | 更新生成元数据                         |
| PUT    | `/api/images/{id}/tags`          | 整体替换图片标签                       |
| POST   | `/api/images/{id}/tags`          | 追加图片标签                           |
| DELETE | `/api/images/{id}/tags`          | 移除指定标签                           |
| POST   | `/api/images/batch/tags`         | 批量添加标签（JDBC 批量插入）          |
| DELETE | `/api/images/batch/tags`         | 批量移除标签                           |
| GET    | `/api/images/{id}/download`      | 下载原图（Content-Disposition 附件）   |
| GET    | `/api/images/{id}/workflow`      | 下载 ComfyUI workflow JSON（若无返回 404） |
| GET    | `/api/folders/tree`              | 获取文件夹树（含图片计数）             |
| GET    | `/api/folders`                   | 获取扁平文件夹列表                     |
| POST   | `/api/folders`                   | 创建文件夹（name、parentId、description）|
| PUT    | `/api/folders/{id}`              | 更新文件夹                             |
| DELETE | `/api/folders/{id}`              | 删除文件夹                             |
| GET    | `/api/tags`                      | 获取所有标签                           |
| GET    | `/api/tags/search?q=`            | 按关键词搜索标签                       |
| POST   | `/api/tags`                      | 创建标签（name, color）                |
| PUT    | `/api/tags/{id}`                 | 更新标签                               |
| DELETE | `/api/tags/{id}`                 | 删除标签（清理图片关联）               |
| GET    | `/api/files/drives`              | 列出根目录（Windows 盘符）             |
| GET    | `/api/files/browse?path=`        | 列出目录内容                           |
| GET    | `/api/files/stats`               | 获取存储使用统计                       |

### 数据库表结构

```
ai_image                                      tag
├── id (BIGINT, PK, AUTO_INCREMENT)            ├── id (BIGINT, PK, AUTO_INCREMENT)
├── original_filename (VARCHAR(500), NN)       ├── name (VARCHAR(100), UNIQUE, NN)
├── stored_filename (VARCHAR(100), UNIQUE)     └── color (VARCHAR(30), DEFAULT '#1890ff')
├── file_path (VARCHAR(1000), NN)
├── thumbnail_path (VARCHAR(1000))             folder
├── file_size (BIGINT)                         ├── id (BIGINT, PK, AUTO_INCREMENT)
├── width (INT)                                ├── name (VARCHAR(200), NN)
├── height (INT)                               ├── description (VARCHAR(500))
├── format (VARCHAR(10))                       ├── parent_id (BIGINT, FK → folder.id)
├── prompt (TEXT)                              ├── created_at (DATETIME, NN)
├── negative_prompt (TEXT)                     └── updated_at (DATETIME, NN)
├── model (VARCHAR(255))
├── steps (INT)                                image_tag (关联表)
├── cfg_scale (DOUBLE)                         ├── image_id (BIGINT, FK → ai_image.id)
├── seed (BIGINT)                              ├── tag_id (BIGINT, FK → tag.id)
├── sampler (VARCHAR(100))
├── workflow_json (MEDIUMTEXT)
├── comfyui_prompt_json (MEDIUMTEXT)                     └── PRIMARY KEY (image_id, tag_id)
├── is_favorite (BOOLEAN, DEFAULT FALSE)
├── folder_id (BIGINT, FK → folder.id)
├── created_at (DATETIME, NN)
└── updated_at (DATETIME, NN)

索引：idx_model_created, idx_is_favorite, idx_folder_id
```

---

## 配置说明

### 后端配置

所有后端配置集中在 `backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_image_manager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: root123
  jpa:
    hibernate:
      ddl-auto: update    # 自动同步表结构（开发环境安全）
    show-sql: true         # 生产环境建议设为 false
  servlet:
    multipart:
      max-file-size: 100MB       # 单文件限制
      max-request-size: 500MB    # 总请求限制

app:
  storage:
    base-path: D:/projects/ai-image-manager/storage
    images-dir: originals
    thumbnails-dir: thumbnails
    thumbnail-max-dim: 300    # 缩略图最大边长（像素）
  scan:
    default-scan-path: D:/AI_Outputs
    allowed-extensions: .png,.jpg,.jpeg,.webp,.gif,.bmp
```

配置要点说明：
- `ddl-auto: update` — Hibernate 自动创建/更新表。开发环境安全，生产环境建议改用 `validate` 配合手动迁移
- `max-file-size: 100MB` — AI 生成图片通常在 50MB 以内
- `max-request-size: 500MB` — 批量上传时的总请求限制
- `thumbnail-max-dim: 300` — 缩略图按比例缩放至 300x300 以内

### 前端配置

**Vite 代理配置**（`frontend/vite.config.ts`）：

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true },
    '/files': { target: 'http://localhost:8080', changeOrigin: true },
  },
}
```

Vite 开发服务器将 `/api/*` 和 `/files/*` 请求代理到 Spring Boot 后端（端口 8080）。生产环境中应使用 nginx 反向代理，或构建前端后由 Spring Boot 直接托管。

**Axios HTTP 客户端**（`src/api/http.ts`）：

- 基础路径：`/api`
- 超时时间：60 秒
- 全局错误拦截器：通过 Element Plus `ElMessage.error()` 显示错误通知

---

## 可用命令

### 后端（在 `backend/` 目录下执行）

| 命令                                | 说明                         |
| ----------------------------------- | ---------------------------- |
| `./mvnw spring-boot:run`            | 启动 Spring Boot 开发服务器（端口 8080） |
| `./mvnw clean install -DskipTests`  | 构建项目（跳过测试）          |
| `./mvnw clean install`              | 构建项目（含测试）            |
| `./mvnw test`                       | 运行测试                      |
| `./mvnw package -DskipTests`        | 打包可执行 JAR                |

### 前端（在 `frontend/` 目录下执行）

| 命令               | 说明                           |
| ------------------ | ------------------------------ |
| `npm run dev`      | 启动 Vite 开发服务器（端口 5173） |
| `npm run build`    | TypeScript 类型检查 + 生产构建 |
| `npm run preview`  | 本地预览生产构建               |
| `npm install`      | 安装依赖                       |

---

## API 参考

### 图片接口

所有端点以 `/api/images` 为前缀。

**搜索图片**

```
GET /api/images?keyword=&folderId=&favorite=&tagIds=&page=0&size=48&sort=createdAt,desc
```

参数说明：
- `keyword` — 在 prompt、negativePrompt、model、originalFilename 中搜索
- `folderId` — 按文件夹 ID 筛选
- `favorite` — 设为 `true` 只显示收藏
- `tagIds` — 逗号分隔的标签 ID（图片必须包含所有指定标签）
- `page` — 页码（从 0 开始）
- `size` — 每页数量（默认 48）
- `sort` — 排序字段和方向，如 `createdAt,desc`、`originalFilename,asc`

返回值：`PageResponse<ImageDto>`

**上传图片**

```
POST /api/images/upload
Content-Type: multipart/form-data
```

参数：
- `files` — 一个或多个图片文件（multipart）
- `prompt`, `model`, `steps`, `cfgScale`, `seed`, `sampler` — 可选元数据（应用于所有上传文件）

返回值：`ImageUploadResponse[]`（每张文件的上传结果 success/error）

**扫描目录**

```
POST /api/images/scan
Content-Type: application/json
{"directoryPath": "D:/AI_Outputs"}
```

扫描本地目录（最多递归 3 层），导入支持的图片文件。

### 文件夹接口

```
GET    /api/folders/tree     嵌套文件夹树（含图片计数）
GET    /api/folders          扁平文件夹列表
POST   /api/folders          创建文件夹
PUT    /api/folders/{id}     更新文件夹
DELETE /api/folders/{id}     删除文件夹
```

### 标签接口

```
GET    /api/tags                   获取所有标签
GET    /api/tags/search?q=         按关键词搜索标签
POST   /api/tags                   创建标签 { name, color }
PUT    /api/tags/{id}              更新标签
DELETE /api/tags/{id}              删除标签（清理图片关联）
```

首次启动时会自动创建 10 个默认标签：

| 标签名称 | 颜色       |
| -------- | ---------- |
| 人像     | `#f43f5e`  |
| 风景     | `#10b981`  |
| 科幻     | `#6366f1`  |
| 奇幻     | `#a855f7`  |
| 二次元   | `#ec4899`  |
| 写实     | `#f59e0b`  |
| 概念艺术 | `#8b5cf6`  |
| 建筑     | `#0ea5e9`  |
| 动物     | `#84cc16`  |
| 抽象     | `#f97316`  |

### 文件系统接口

```
GET /api/files/drives            列出根目录（Windows 盘符）
GET /api/files/browse?path=      列出目录内容
GET /api/files/stats             存储使用统计 { usedBytes, imageCount, thumbnailCount }
```

---

## 项目结构详解

### 前端状态管理（Pinia）

三个 Store 管理应用状态：

- **imageStore** — 图片列表、分页、筛选（关键词、文件夹、收藏、标签）、选中状态、批量操作
- **tagStore** — 标签列表、CRUD 操作
- **folderStore** — 文件夹树、CRUD 操作

### 前端路由

| 路径           | 视图              | 说明                         |
| -------------- | ----------------- | ---------------------------- |
| `/`            | GalleryView       | 图片画廊（筛选、排序、分页） |
| `/image/:id`   | ImageDetailView   | 图片详情（元数据、标签管理） |
| `/upload`      | UploadView        | 上传图片、扫描目录           |
| `/tags`        | TagManageView     | 标签管理（CRUD）             |

### 关键前端组件

- **ImageGrid** — 响应式自适应网格（最小列宽 200px），加载时显示 12 个骨架屏，空状态引导用户上传
- **ImageCard** — 缩略图、文件名、尺寸、文件大小、收藏按钮、删除按钮、最多 3 个标签角标
- **BatchActions** — 浮动批量操作栏（显示选择数量），含标签管理对话框和删除确认
- **SearchBar** — 300ms 防抖关键词搜索，带清除筛选按钮
- **AppSidebar** — 文件夹树筛选、收藏切换、标签筛选列表
- **DirectoryPicker** — 模态框文件浏览器（盘符 → 目录），支持返回导航、图片数量显示、选择确认

### 元数据提取

`MetadataExtractorService` 使用**原始二进制解析** PNG 的 tEXt/iTXt 块（替代 `javax.imageio`，避免大块截断问题），支持以下格式：

1. **A1111 格式**（key: `parameters` 或 `Description`）：
   ```
   prompt\nNegative prompt: neg\nSteps: 20, Sampler: DPM++ 2M Karras, CFG scale: 7, Seed: 123, Size: 1024x1024, Model: SDXL 1.0
   ```

2. **ComfyUI 格式**（key: `prompt`）：JSON 节点图自动解包，支持 **SDXL** 和 **Flux.2** 工作流：
   - 查找主采样节点：优先 `KSampler`/`KSamplerAdvanced`，其次 `UltimateSDUpscale`（Flux 放大），回退到任意含 `sampler_name` + `denoise` 的节点
   - 沿引用链追踪到 `CLIPTextEncode` → 提取正面/负面提示词；支持 `CR Text Concatenate` 等多段文本拼接
   - 沿 model 引用链回溯：`CheckpointLoaderSimple`（`ckpt_name`）→ SDXL；`UNETLoader`（`unet_name`）→ Flux；`LoraLoaderModelOnly` → 基础模型 + LoRA 名称
   - 定位 `EmptyLatentImage` → 提取宽高；也处理 `LoadImage`（图生图工作流）
   - 完整 workflow JSON 存入 `workflowJson` 字段，完整 prompt JSON 存入 `comfyuiPromptJson` 字段

3. 用户上传时提供的元数据优先级高于提取的元数据

元数据提取为"尽力而为"策略，解析失败时静默跳过，不影响上传流程。

### 批量标签性能

批量添加标签使用 `JdbcTemplate.batchUpdate` + `INSERT IGNORE`，将 `图片ID × 标签ID` 的笛卡尔积一次性批量插入，避免 N+1 查询问题。例如为 10 张图片添加 3 个标签，执行一次 30 条记录的批量插入。

### 错误处理

后端使用 `@RestControllerAdvice` 统一错误响应格式：

```json
{
  "error": "not_found",
  "message": "Image not found with id: 42",
  "timestamp": "2026-05-06T22:20:58.844"
}
```

错误码：`not_found`、`invalid_file`、`storage_error`、`validation_failed`、`internal_error`。

前端通过 Axios 拦截所有 HTTP 错误，通过 Element Plus 通知组件展示。

---

## 部署指南

### Docker 部署

项目本身不包含 Dockerfile，以下是推荐的部署方案：

```dockerfile
# Dockerfile（后端）
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/ .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: ai_image_manager
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/ai_image_manager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123
      APP_STORAGE_BASE_PATH: /app/storage
    volumes:
      - storage_data:/app/storage

  frontend:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./frontend/dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - backend

volumes:
  mysql_data:
  storage_data:
```

### 手动部署

**后端：**

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://production-host:3306/ai_image_manager?useSSL=true \
  --spring.datasource.username=prod_user \
  --spring.datasource.password=prod_password \
  --spring.jpa.show-sql=false \
  --app.storage.base-path=/var/data/ai-image-manager
```

**前端：**

```bash
cd frontend
npm run build
# 构建产物在 frontend/dist/，使用 nginx 或其他静态文件服务器托管
```

**生产环境 Nginx 配置：**

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /path/to/frontend/dist;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 文件服务
    location /files/ {
        proxy_pass http://localhost:8080;
    }
}
```

---

## 常见问题排查

### 数据库连接失败

**错误信息：** `CommunicationsException: Communications link failure`

解决方案：
1. 确认 MySQL 已启动：`mysql -u root -proot123 -e "SELECT 1"`
2. 检查 MySQL 端口：`netstat -an | grep 3306`
3. 确保数据库已创建：`mysql -u root -proot123 -e "CREATE DATABASE IF NOT EXISTS ai_image_manager"`

### Hibernate 表结构错误

**错误信息：** `Table 'ai_image_manager.ai_image' doesn't exist`

Hibernate 应通过 `ddl-auto: update` 自动创建表。如果失败：
1. 确认 MySQL 用户有 CREATE/ALTER 权限
2. 检查 `application.yml` 语法
3. 重启后端

### Maven 构建失败

```bash
# 清理 Maven 缓存
rm -rf ~/.m2/repository/com/aiimage
cd backend && ./mvnw clean install -DskipTests
```

### 前端代理异常

**现象：** API 调用返回 404 或连接拒绝

解决方案：
1. 确认后端已在 8080 端口运行
2. 检查 `vite.config.ts` 中的代理配置
3. 在浏览器开发者工具中查看实际请求 URL

### 上传失败

**错误信息：** `MultipartException: Current request is not a multipart request`

解决方案：
1. 确保表单使用 `enctype="multipart/form-data"`
2. 检查 `application.yml` 中的 `max-file-size` 和 `max-request-size`
3. 确认存储目录存在且可写入

### 缩略图生成失败

Thumbnailator 可能在处理损坏或异常的图片文件时失败。检查服务器日志。缩略图生成失败会导致整个上传失败。

### 标签操作说明

批量标签操作使用原生 JDBC `INSERT IGNORE`，绕过 JPA 缓存。当前设计中批量标签在独立事务中运行，不会产生脏读问题。

### 删除标签

删除标签时会先通过原生 SQL 清理 `image_tag` 关联表（`DELETE FROM image_tag WHERE tag_id = ?`），再通过原生 SQL 删除标签本身，避免 JPA 级联问题。

### 已知限制

- `storage/` 目录被 git 忽略。新环境上后端启动时会自动创建存储目录
- 批量标签对话框只显示选中图片的**共同标签**（交集），单张图片独有的标签不展示
- 目录扫描最多递归 3 层，仅导入 `app.scan.allowed-extensions` 中配置的格式
- `StorageException` 可能因磁盘已满、路径权限不足或文件被占用而抛出，服务器日志中有详细堆栈信息
