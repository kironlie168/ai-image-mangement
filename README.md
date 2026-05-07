# AI Image Manager

全栈 Web 应用，用于整理、浏览和管理 AI 生成的图片（Stable Diffusion、Midjourney、DALL-E 等）。支持自动从 PNG 文件中提取生成元数据、文件夹树组织、彩色标签分类和批量操作。

## 主要功能

- **图片画廊** — 自适应网格视图、分页排序、多选操作
- **搜索与筛选** — 按提示词/模型/文件名搜索；按文件夹/收藏/标签筛选
- **AI 元数据提取** — 自动从 PNG tEXt/iTXt 块中提取生成参数（A1111 / ComfyUI 格式均支持，含 SDXL 和 Flux.2）
- **文件夹树** — 嵌套文件夹组织，支持图片计数
- **彩色标签** — 创建和管理彩色标签，支持批量分配/移除
- **批量操作** — 多选图片进行批量标签、批量移除标签、批量删除
- **上传与扫描** — 单张上传并附加元数据，或扫描本地目录批量导入
- **文件系统浏览器** — 在浏览器中浏览本地磁盘和目录，选择扫描路径
- **收藏系统** — 标记收藏图片并按收藏筛选
- **存储统计** — 查看磁盘使用量、图片数量和缩略图数量
- **Swagger API 文档** — 完整的 OpenAPI 文档，访问 `/swagger-ui.html`
- **工作流下载** — 支持下载 ComfyUI 工作流 JSON 文件

## 目录

- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [架构说明](#架构说明)
  - [目录结构](#目录结构)
  - [数据流](#数据流)
  - [API 端点一览](#api-端点一览)
  - [数据库表结构](#数据库表结构)
- [配置说明](#配置说明)
  - [环境变量](#环境变量)
  - [后端配置](#后端配置)
  - [前端配置](#前端配置)
- [可用命令](#可用命令)
- [API 参考](#api-参考)
  - [图片接口](#图片接口)
  - [文件夹接口](#文件夹接口)
  - [标签接口](#标签接口)
  - [文件系统接口](#文件系统接口)
- [已知问题 / 坑点](#已知问题--坑点)
- [部署指南](#部署指南)
  - [Docker 部署](#docker-部署)
  - [手动部署](#手动部署)
- [常见问题排查](#常见问题排查)
- [贡献指南](#贡献指南)
- [许可](#许可)

---

## 技术栈

| 层级 | 技术 | 版本 |
| --- | --- | --- |
| **后端** | Java (OpenJDK) | 21 |
| | Spring Boot | 4.0.6 |
| | Spring Data JPA / Hibernate | 7.2 |
| | MySQL Connector | (runtime) |
| | Lombok | (optional) |
| | Thumbnailator (缩略图) | 0.4.20 |
| | SpringDoc OpenAPI (Swagger) | 2.8.5 |
| **前端** | Vue | 3.5 |
| | TypeScript | 6.0 |
| | Vite | 8.0 |
| | Vue Router | 5.0 |
| | Pinia (状态管理) | 3.0 |
| | Element Plus (UI 组件库) | 2.13 |
| | Axios (HTTP 客户端) | 1.16 |
| **数据库** | MySQL | 8.0+ |
| **构建工具** | Maven (后端) | (Wrapper 3.x) |
| | npm (前端) | 10+ |

---

## 环境要求

- **Java 21** 或更高版本（推荐 Eclipse Temurin / OpenJDK 21）
- **Node.js 20+** 和 **npm 10+**
- **MySQL 8.0+**（可通过 Docker 运行，或本地安装）
- 至少 **1 GB 内存**（供 Maven + Spring Boot 使用）
- 用于存储图片的本地目录（默认 `D:/projects/ai-image-manager/storage`，可在配置中修改）

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

或使用本地 MySQL：

```bash
mysql -u root -p
CREATE DATABASE IF NOT EXISTS ai_image_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 后端配置与启动

```bash
cd backend

# 首次构建（跳过测试以加快速度）
./mvnw clean install -DskipTests

# 启动后端（端口 8080）
./mvnw spring-boot:run
```

首次启动可能需要 30–60 秒（Maven 下载依赖 + Hibernate 初始化数据库）。看到以下日志即为启动成功：

```
Started AiImageManagerApplication in 5.218 seconds
```

> **注意**: Hibernate 的 `ddl-auto: update` 会自动创建表结构，无需手动执行 SQL。

### 4. 前端配置与启动

打开新终端：

```bash
cd frontend
npm install

# 启动 Vite 开发服务器（端口 5173）
npm run dev
```

看到以下输出即为启动成功：

```
VITE v8.0.10  ready in 561 ms
  ➜  Local:   http://localhost:5173/
```

### 5. 打开浏览器

- **应用主页**: [http://localhost:5173](http://localhost:5173)
- **Swagger API 文档**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 架构说明

### 目录结构

```
ai-image-manager/
├── backend/                              # Spring Boot 后端应用
│   └── src/main/java/com/aiimage/
│       ├── AiImageManagerApplication.java    # 应用入口 (@SpringBootApplication)
│       ├── config/
│       │   ├── CorsConfig.java               # CORS 跨域配置（允许 localhost:5173）
│       │   ├── WebConfig.java                # 静态资源映射 /files/**
│       │   └── SwaggerConfig.java            # OpenAPI 文档配置
│       ├── controller/
│       │   ├── ImageController.java          # /api/images — 图片 CRUD、搜索、上传、扫描、批量操作、工作流下载
│       │   ├── FolderController.java         # /api/folders — 文件夹树 CRUD
│       │   ├── TagController.java            # /api/tags — 标签 CRUD
│       │   └── FileController.java           # /api/files — 文件系统浏览、存储统计
│       ├── service/
│       │   ├── ImageService.java             # 图片业务逻辑，批量标签使用 JDBC batchUpdate
│       │   ├── FolderService.java            # 文件夹树构建，嵌套 DTO 转换
│       │   ├── TagService.java               # 标签 CRUD，默认标签种子数据（10 个）
│       │   ├── FileStorageService.java       # 文件 I/O、缩略图生成、目录扫描
│       │   └── MetadataExtractorService.java # PNG tEXt/iTXt 元数据解析（A1111 / ComfyUI）
│       ├── entity/
│       │   ├── AiImage.java                  # 图片 JPA 实体（含生成元数据字段）
│       │   ├── Folder.java                   # 层级文件夹实体（自引用 parent-children）
│       │   └── Tag.java                      # 彩色标签实体（name + color）
│       ├── repository/
│       │   ├── AiImageRepository.java        # 自定义 @Query 搜索（关键词/文件夹/收藏/标签综合筛选）
│       │   ├── FolderRepository.java         # 树形查询（findByParentIdIsNull）
│       │   └── TagRepository.java            # 标签搜索，原生删除并清理图片关联
│       ├── dto/                              # Java Record — API 请求/响应
│       │   ├── ImageDto.java                 # 图片响应（含标签和文件夹信息）
│       │   ├── ImagePreviewResponse.java     # 文件预读响应（上传前的元数据预览）
│       │   ├── ImageUploadResponse.java      # 上传结果（每张文件 success/error）
│       │   ├── PageResponse.java             # 通用分页响应包装
│       │   ├── FolderDto.java                # 文件夹响应（含嵌套子文件夹和图片数）
│       │   ├── TagDto.java                   # 标签响应
│       │   ├── ScanRequest.java              # 目录扫描请求体
│       │   ├── BatchTagRequest.java          # 批量标签请求
│       │   ├── BatchRemoveTagRequest.java    # 批量移除标签请求
│       │   ├── CreateTagRequest.java         # 创建标签请求
│       │   ├── UpdateTagRequest.java         # 更新标签请求
│       │   └── UpdateMetadataRequest.java    # 元数据更新请求
│       └── exception/
│           ├── GlobalExceptionHandler.java   # @RestControllerAdvice 统一异常处理
│           ├── ResourceNotFoundException.java
│           ├── InvalidFileException.java
│           └── StorageException.java
│
├── frontend/                             # Vue 3 + TypeScript 前端
│   ├── src/
│   │   ├── main.ts                          # 应用入口：Pinia + Router + Element Plus 注册
│   │   ├── App.vue                          # 根布局：AppNavbar、AppSidebar、router-view
│   │   ├── router/index.ts                  # 4 个路由（gallery, image-detail, upload, tags）
│   │   ├── types/image.ts                   # TypeScript 接口定义（ImageDto, TagDto, FolderDto 等）
│   │   ├── api/                             # Axios HTTP 客户端
│   │   │   ├── http.ts                      # Axios 实例（baseURL: /api, timeout: 60s, 错误拦截器）
│   │   │   ├── imageApi.ts                  # 图片接口
│   │   │   ├── tagApi.ts                    # 标签接口
│   │   │   ├── folderApi.ts                 # 文件夹接口
│   │   │   └── fileApi.ts                   # 文件系统接口
│   │   ├── stores/                          # Pinia 状态管理
│   │   │   ├── imageStore.ts                # 图片列表/详情/筛选/分页/选中/批量操作
│   │   │   ├── tagStore.ts                  # 标签列表/CRUD
│   │   │   └── folderStore.ts               # 文件夹树/CRUD
│   │   ├── views/                           # 页面组件
│   │   │   ├── GalleryView.vue              # 图片画廊（搜索/排序/分页/批量操作栏）
│   │   │   ├── ImageDetailView.vue          # 图片详情（元数据查看/编辑/标签管理）
│   │   │   ├── UploadView.vue               # 上传页面（文件上传/目录扫描/存储统计）
│   │   │   └── TagManageView.vue            # 标签管理（CRUD/颜色选择器/搜索）
│   │   ├── components/                      # 可复用 UI 组件
│   │   │   ├── AppNavbar.vue                # 顶部导航栏（Gallery / Upload / Tags）
│   │   │   ├── AppSidebar.vue               # 侧边栏（文件夹树/收藏筛选/标签筛选）
│   │   │   ├── PageHeader.vue               # 页面标题组件（标题 + 副标题 + 渐变装饰线）
│   │   │   ├── ImageGrid.vue                # 响应式图片网格（骨架屏加载 / 空状态引导）
│   │   │   ├── ImageCard.vue                # 图片卡片（缩略图/文件名/尺寸/标签/收藏/删除）
│   │   │   ├── SearchBar.vue                # 防抖搜索框 + 清除筛选按钮
│   │   │   ├── BatchActions.vue             # 浮动批量操作栏（标签对话框/删除确认）
│   │   │   ├── TagSelector.vue              # 多选标签下拉组件
│   │   │   ├── FolderTree.vue               # el-tree 封装文件夹选择
│   │   │   └── DirectoryPicker.vue          # 文件系统浏览器模态框（盘符 → 目录选择）
│   │   └── utils/                           # 工具函数
│   │       ├── format.ts                    # formatFileSize() — 文件大小可读格式化
│   │       ├── color.ts                     # textColor() / hexToRgb() — 颜色工具
│   │       └── download.ts                  # downloadBlob() — 浏览器文件下载
│   ├── index.html                           # Vite 入口 HTML
│   ├── vite.config.ts                       # Vite 配置（@ 别名、/api + /files 代理）
│   └── package.json                         # 前端依赖配置
│
└── storage/                              # 图片存储目录（git 忽略）
    ├── originals/                           # 原始图片（UUID 重命名存储）
    └── thumbnails/                          # 缩略图（最大 300px，自动生成）
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

| 方法   | 端点                             | 说明                                     |
| ------ | -------------------------------- | ---------------------------------------- |
| GET    | `/api/images`                    | 搜索图片（关键词/文件夹/收藏/标签/分页/排序） |
| GET    | `/api/images/{id}`               | 获取单张图片详情                        |
| POST   | `/api/images/upload`             | 上传图片（multipart，单文件最大 100MB） |
| POST   | `/api/images/scan`               | 扫描本地目录并导入图片                  |
| DELETE | `/api/images/{id}`               | 删除单张图片及其文件                    |
| DELETE | `/api/images/batch`              | 批量删除图片                            |
| PUT    | `/api/images/{id}/favorite`      | 切换收藏状态                            |
| PUT    | `/api/images/{id}/metadata`      | 更新生成元数据                          |
| PUT    | `/api/images/{id}/tags`          | 整体替换图片标签                        |
| POST   | `/api/images/{id}/tags`          | 追加图片标签                            |
| DELETE | `/api/images/{id}/tags`          | 移除指定标签                            |
| POST   | `/api/images/batch/tags`         | 批量添加标签（JDBC 批量插入）           |
| DELETE | `/api/images/batch/tags`         | 批量移除标签                            |
| GET    | `/api/images/{id}/download`      | 下载原图（Content-Disposition 附件）     |
| GET    | `/api/images/{id}/workflow`      | 下载 ComfyUI workflow JSON（若无返回 404） |
| GET    | `/api/folders/tree`              | 获取文件夹树（含图片计数）              |
| GET    | `/api/folders`                   | 获取扁平文件夹列表                      |
| POST   | `/api/folders`                   | 创建文件夹                              |
| PUT    | `/api/folders/{id}`              | 更新文件夹                              |
| DELETE | `/api/folders/{id}`              | 删除文件夹                              |
| GET    | `/api/tags`                      | 获取所有标签                            |
| GET    | `/api/tags/search?q=`            | 按关键词搜索标签                        |
| POST   | `/api/tags`                      | 创建标签                                |
| PUT    | `/api/tags/{id}`                 | 更新标签                                |
| DELETE | `/api/tags/{id}`                 | 删除标签（自动清理图片关联）            |
| GET    | `/api/files/drives`              | 列出根目录（Windows 盘符）              |
| GET    | `/api/files/browse?path=`        | 列出目录内容                            |
| GET    | `/api/files/stats`               | 获取存储使用统计                        |

### 数据库表结构

```sql
-- 核心表：图片
ai_image
├── id              BIGINT(19)       PK, AUTO_INCREMENT
├── original_filename VARCHAR(500)   NN               -- 原始文件名
├── stored_filename   VARCHAR(100)   UNIQUE, NN        -- UUID 存储名
├── file_path         VARCHAR(1000)  NN                -- 存储路径
├── thumbnail_path    VARCHAR(1000)                    -- 缩略图路径
├── file_size         BIGINT(19)                       -- 字节数
├── width             INT(10)                          -- 图片宽度
├── height            INT(10)                          -- 图片高度
├── format            VARCHAR(10)                      -- 文件格式
├── prompt            TEXT                             -- 生成提示词
├── negative_prompt   TEXT                             -- 反向提示词
├── model             VARCHAR(255)                     -- AI 模型名称
├── steps             INT(10)                          -- 采样步数
├── cfg_scale         DOUBLE                           -- CFG 比例
├── seed              BIGINT(19)                       -- 随机种子
├── sampler           VARCHAR(100)                     -- 采样器
├── workflow_json     MEDIUMTEXT                       -- ComfyUI 完整工作流 JSON
├── comfyui_prompt_json MEDIUMTEXT                     -- ComfyUI 提示词 JSON
├── is_favorite       BOOLEAN         DEFAULT FALSE     -- 收藏标记
├── folder_id         BIGINT(19)      FK → folder.id   -- 所属文件夹
├── created_at        DATETIME        NN                -- 创建时间
└── updated_at        DATETIME        NN                -- 更新时间

索引: idx_model_created(model, created_at), idx_is_favorite(is_favorite), idx_folder_id(folder_id)

-- 关联表：图片与标签多对多
image_tag
├── image_id  BIGINT(19)  FK → ai_image.id
└── tag_id    BIGINT(19)  FK → tag.id
  → PRIMARY KEY (image_id, tag_id)

-- 标签
tag
├── id         BIGINT(19)      PK, AUTO_INCREMENT
├── name       VARCHAR(100)    UNIQUE, NN
├── color      VARCHAR(30)     DEFAULT '#1890ff'
└── created_at DATETIME        NN

-- 文件夹（自引用树结构）
folder
├── id          BIGINT(19)      PK, AUTO_INCREMENT
├── name        VARCHAR(200)    NN
├── description VARCHAR(500)
├── parent_id   BIGINT(19)      FK → folder.id  (null = 根文件夹)
├── created_at  DATETIME        NN
└── updated_at  DATETIME        NN
```

---

## 配置说明

### 环境变量

后端目前使用 `application.yml` 硬编码配置，没有通过环境变量注入（Spring Boot 支持 `SPRING_*` / `APP_*` 环境变量覆盖，详见下方映射表）：

| 环境变量 | 对应 YAML 路径 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/ai_image_manager?useSSL=false&...` | MySQL 连接地址 |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `root` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `root123` | 数据库密码 |
| `SPRING_JPA_SHOW_SQL` | `spring.jpa.show-sql` | `true` | 是否打印 SQL |
| `APP_STORAGE_BASE_PATH` | `app.storage.base-path` | `D:/projects/ai-image-manager/storage` | 图片存储根目录 |
| `APP_STORAGE_THUMBNAIL_MAX_DIM` | `app.storage.thumbnail-max-dim` | `300` | 缩略图最大边长 |
| `APP_SCAN_DEFAULT_SCAN_PATH` | `app.scan.default-scan-path` | `D:/AI_Outputs` | 默认扫描路径 |
| `APP_SCAN_ALLOWED_EXTENSIONS` | `app.scan.allowed-extensions` | `.png,.jpg,.jpeg,.webp,.gif,.bmp` | 允许导入的扩展名 |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `spring.servlet.multipart.max-file-size` | `100MB` | 单文件上传上限 |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | `spring.servlet.multipart.max-request-size` | `500MB` | 总请求上传上限 |

### 后端配置

配置文件: `backend/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_image_manager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf-8
    username: root
    password: root123
  jpa:
    hibernate:
      ddl-auto: update    # 开发环境自动同步表结构; 生产建议改为 validate
    show-sql: true         # 生产建议 false
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 500MB

app:
  storage:
    base-path: D:/projects/ai-image-manager/storage
    thumbnail-max-dim: 300
  scan:
    default-scan-path: D:/AI_Outputs
    allowed-extensions: .png,.jpg,.jpeg,.webp,.gif,.bmp
```

**配置要点：**
- `ddl-auto: update` — Hibernate 自动创建/更新表结构。开发环境安全；生产环境建议改为 `validate` 配合手动迁移管理
- `max-file-size: 100MB` — AI 生成图片通常在 50MB 以内
- `thumbnail-max-dim: 300` — 缩略图按比例缩放至 300x300 以内
- `allowed-extensions` — 扫描时只导入这些扩展名的文件

### 前端配置

**Vite 代理** (`frontend/vite.config.ts`)：

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true },
    '/files': { target: 'http://localhost:8080', changeOrigin: true },
  },
}
```

Vite 开发服务器将 `/api/*` 和 `/files/*` 请求代理到 Spring Boot 后端（端口 8080）。生产环境中应使用 nginx 反向代理，或将前端构建产物由 Spring Boot 直接托管。

**Axios HTTP 客户端** (`frontend/src/api/http.ts`)：
- 基础路径: `/api`
- 超时时间: 60 秒
- 全局错误拦截器: 通过 Element Plus `ElMessage.error()` 显示错误通知（自动捕获所有 HTTP 错误）

---

## 可用命令

### 后端（在 `backend/` 目录下执行）

| 命令 | 说明 |
| --- | --- |
| `./mvnw spring-boot:run` | 启动 Spring Boot 开发服务器（端口 8080） |
| `./mvnw clean install -DskipTests` | 构建项目（跳过测试） |
| `./mvnw clean install` | 构建项目（含测试） |
| `./mvnw test` | 运行测试 |
| `./mvnw package -DskipTests` | 打包可执行 JAR |

### 前端（在 `frontend/` 目录下执行）

| 命令 | 说明 |
| --- | --- |
| `npm run dev` | 启动 Vite 开发服务器（端口 5173） |
| `npm run build` | TypeScript 类型检查 + 生产构建 |
| `npm run preview` | 本地预览生产构建 |
| `npm install` | 安装依赖 |

---

## API 参考

### 图片接口

所有端点以 `/api/images` 为前缀。

**搜索图片**

```
GET /api/images?keyword=&folderId=&favorite=&tagIds=&page=0&size=48&sort=createdAt,desc
```

参数说明：
| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | string | 在 prompt、negativePrompt、model、originalFilename 中模糊搜索 |
| `folderId` | long | 按文件夹 ID 筛选 |
| `favorite` | boolean | `true` 只显示收藏 |
| `tagIds` | string | 逗号分隔的标签 ID（图片必须包含所有指定标签，逻辑与） |
| `page` | int | 页码（从 0 开始） |
| `size` | int | 每页数量（默认 48） |
| `sort` | string | 排序字段和方向，如 `createdAt,desc`、`originalFilename,asc` |

返回值: `PageResponse<ImageDto>`

```json
{
  "content": [
    {
      "id": 1,
      "originalFilename": "example.png",
      "thumbnailUrl": "/files/thumbnails/uuid.png",
      "imageUrl": "/files/originals/uuid.png",
      "fileSize": 4528321,
      "width": 1024,
      "height": 1024,
      "format": "png",
      "prompt": "a beautiful landscape",
      "negativePrompt": "blurry, low quality",
      "model": "SDXL 1.0",
      "steps": 20,
      "cfgScale": 7.0,
      "seed": 123456789,
      "sampler": "DPM++ 2M Karras",
      "workflowJson": "{...}",
      "comfyuiPromptJson": "{...}",
      "isFavorite": false,
      "folderId": 1,
      "folderName": "landscapes",
      "tags": [{"id": 1, "name": "风景", "color": "#10b981"}],
      "createdAt": "2026-05-06T22:20:58",
      "updatedAt": "2026-05-06T22:20:58"
    }
  ],
  "page": 0,
  "size": 48,
  "totalElements": 100,
  "totalPages": 3
}
```

**上传图片**

```
POST /api/images/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `files` | multipart[] | 一个或多个图片文件 |
| `prompt` | string | 可选：生成提示词（应用于所有上传文件） |
| `model` | string | 可选：模型名称 |
| `steps` | int | 可选：采样步数 |
| `cfgScale` | double | 可选：CFG 比例 |
| `seed` | long | 可选：随机种子 |
| `sampler` | string | 可选：采样器名称 |

返回值: `ImageUploadResponse[]`（每张文件 success/error）

**扫描目录**

```
POST /api/images/scan
Content-Type: application/json

{"directoryPath": "D:/AI_Outputs"}
```

扫描本地目录（最多递归 3 层），导入支持的图片文件。自动提取 PNG 元数据。

**其他图片接口**

```
GET    /api/images/{id}          获取单张图片详情
PUT    /api/images/{id}/favorite  切换收藏状态
PUT    /api/images/{id}/metadata  更新生成元数据
PUT    /api/images/{id}/tags      整体替换图片标签
POST   /api/images/{id}/tags      追加图片标签
DELETE /api/images/{id}/tags      移除指定标签
DELETE /api/images/{id}           删除单张图片及文件
DELETE /api/images/batch          批量删除
POST   /api/images/batch/tags     批量添加标签
DELETE /api/images/batch/tags     批量移除标签
GET    /api/images/{id}/download  下载原图
GET    /api/images/{id}/workflow  下载 ComfyUI 工作流 JSON
```

### 文件夹接口

```
GET    /api/folders/tree     嵌套文件夹树（含图片计数）
GET    /api/folders          扁平文件夹列表
POST   /api/folders          创建文件夹 { name, parentId?, description? }
PUT    /api/folders/{id}     更新文件夹
DELETE /api/folders/{id}     删除文件夹
```

### 标签接口

```
GET    /api/tags                   获取所有标签
GET    /api/tags/search?q=         按关键词搜索标签
POST   /api/tags                   创建标签 { name, color }
PUT    /api/tags/{id}              更新标签
DELETE /api/tags/{id}              删除标签（自动清理图片关联）
```

首次启动时会自动创建 10 个默认标签：

| 标签名称 | 颜色 | 色值 |
| --- | --- | --- |
| 人像 | 玫红 | `#f43f5e` |
| 风景 | 翠绿 | `#10b981` |
| 科幻 | 靛蓝 | `#6366f1` |
| 奇幻 | 紫色 | `#a855f7` |
| 二次元 | 粉红 | `#ec4899` |
| 写实 | 橙色 | `#f59e0b` |
| 概念艺术 | 紫罗兰 | `#8b5cf6` |
| 建筑 | 天蓝 | `#0ea5e9` |
| 动物 | 青柠 | `#84cc16` |
| 抽象 | 橙红 | `#f97316` |

### 文件系统接口

```
GET /api/files/drives            列出根目录（Windows 盘符如 C:/ D:/）
GET /api/files/browse?path=      列出目录内容（文件名、大小、修改时间、是否为目录）
GET /api/files/stats             存储统计 { usedBytes, imageCount, thumbnailCount }
```

---

## 已知问题 / 坑点

### 元数据提取

- `MetadataExtractorService` 使用**原始二进制解析** PNG 的 tEXt/iTXt 块（替代 `javax.imageio`，避免大块截断问题）
- **A1111 格式**: key 为 `parameters` 或 `Description`，解析 prompt、反向提示词、采样参数
- **ComfyUI 格式**: key 为 `prompt`，JSON 节点图自动解包，支持 SDXL 和 Flux.2 工作流：
  - 优先查找 `KSampler` / `KSamplerAdvanced` 节点，其次 `UltimateSDUpscale`（Flux 放大），回退到任意含 `sampler_name` + `denoise` 的节点
  - 沿引用链追踪 `CLIPTextEncode` → 提取提示词；支持 `CR Text Concatenate` 等多段拼接
  - 沿 model 引用链回溯：`CheckpointLoaderSimple` → SDXL；`UNETLoader` → Flux；`LoraLoaderModelOnly` → LoRA 叠加
  - 完整 workflow JSON 存入 `workflowJson`，完整 prompt JSON 存入 `comfyuiPromptJson`
- 用户上传时提供的元数据优先级高于自动提取的元数据
- 元数据提取为"尽力而为"策略，解析失败时静默跳过

### 批量操作性能

批量添加标签使用 `JdbcTemplate.batchUpdate` + `INSERT IGNORE`，将 `图片ID × 标签ID` 的笛卡尔积一次性批量插入。例如为 10 张图片添加 3 个标签，执行一次 30 条记录的批量插入，避免 N+1 查询问题。

### 常见陷阱

- **需要 MySQL 8+ 运行中**, 默认连接 `localhost:3306/ai_image_manager`。数据库必须手动创建
- **目录扫描最多递归 3 层**, 仅导入 `app.scan.allowed-extensions` 中配置的格式（默认 .png/.jpg/.jpeg/.webp/.gif/.bmp）
- **`storage/` 目录被 git 忽略**。新环境上后端启动时会自动创建存储目录
- **Thumbnailator 可能在处理损坏图片时失败**, 会导致整个上传失败。检查服务器日志定位原因
- **批量标签对话框只显示选中图片的共同标签（交集）**, 单张图片独有的标签不展示
- **首次启动后端需 Hibernate `ddl-auto: update` 自动建表**, 需确保 MySQL 用户有 CREATE/ALTER 权限
- **`StorageException`** 可能因磁盘已满、路径权限不足或文件被占用而抛出，服务器日志中有详细堆栈信息
- **错误响应格式**:
  ```json
  { "error": "not_found", "message": "Image not found with id: 42", "timestamp": "2026-05-06T22:20:58.844" }
  ```
  错误码: `not_found`、`invalid_file`、`storage_error`、`validation_failed`、`internal_error`

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

    # SPA 路由回退（必须）
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

**错误:** `CommunicationsException: Communications link failure`

**排查步骤：**
1. 确认 MySQL 已启动: `mysql -u root -proot123 -e "SELECT 1"`
2. 检查 MySQL 端口: `netstat -an | grep 3306`
3. 确保数据库已创建: `mysql -u root -proot123 -e "CREATE DATABASE IF NOT EXISTS ai_image_manager"`
4. 检查 `application.yml` 中的连接地址和凭据

### Hibernate 表结构错误

**错误:** `Table 'ai_image_manager.ai_image' doesn't exist`

Hibernate 应通过 `ddl-auto: update` 自动创建表。如果失败：
1. 确认 MySQL 用户有 CREATE/ALTER 权限
2. 检查 `application.yml` 语法
3. 重启后端应用

### Maven 构建失败

```bash
# 清理 Maven 缓存
rm -rf ~/.m2/repository/com/aiimage
cd backend && ./mvnw clean install -DskipTests
```

### 前端代理异常

**现象:** API 调用返回 404 或连接拒绝

**排查步骤：**
1. 确认后端已在 8080 端口运行
2. 检查 `frontend/vite.config.ts` 中的代理配置
3. 在浏览器开发者工具中查看实际请求 URL

### 上传失败

**错误:** `MultipartException: Current request is not a multipart request`

**排查步骤：**
1. 确保表单使用 `enctype="multipart/form-data"`
2. 检查 `application.yml` 中的 `max-file-size` 和 `max-request-size`
3. 确认存储目录存在且可写入

### 缩略图生成失败

Thumbnailator 可能在处理损坏或异常的图片文件时失败。检查服务器日志中的具体异常堆栈。缩略图生成失败会导致整个上传失败。

### 标签操作说明

批量标签操作使用原生 JDBC `INSERT IGNORE`，绕过 JPA 缓存。当前设计中批量标签在独立事务中运行，不会产生脏读问题。

### 删除标签

删除标签时会先通过原生 SQL 清理 `image_tag` 关联表（`DELETE FROM image_tag WHERE tag_id = ?`），再通过原生 SQL 删除标签本身，避免 JPA 级联问题。

---

## 贡献指南

欢迎贡献！请遵循以下流程：

1. Fork 本仓库
2. 创建特性分支: `git checkout -b feat/your-feature`
3. 提交改动: `git commit -m "feat: add your feature"`
4. 推送到远程: `git push origin feat/your-feature`
5. 创建 Pull Request

### 开发规范

- 后端遵循 Spring Boot 项目约定: Controller → Service → Repository 分层
- 前端遵循 Vue 3 Composition API + `<script setup>` 风格
- DTO 使用 Java Record 定义
- API 路径使用 RESTful 命名规范
- 提交信息遵循 Conventional Commits 规范

---

## 许可

MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
