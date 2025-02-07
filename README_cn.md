# Omni-Flow 项目

<div align="center">

[English](./README.md) / 简体中文

</div>

## 目录
- [简介](#简介)
- [环境要求](#环境要求)
- [安装](#安装)
- [使用](#使用)
- [贡献](#贡献)
- [许可证](#许可证)

## 简介
Omni-Flow 是一个用于编排和调度 AI 能力的控制器，旨在简化 AI 模型的管理和任务调度。通过 JSON 配置文件和 API 调用，Omni-Flow 可以灵活地调度多种 AI 模型，生成所需的媒体内容。该项目灵感来源于 Kubernetes，并基于 Spring Boot 框架构建，底层存储使用 Redis。

## 环境要求
为了运行本项目，您需要以下环境：
- **Java**：21 或更高版本
- **Maven**：3.x 或更高版本
- **Redis**：5.0 或更高版本（需支持 Stream）
- **Docker**（可选）：用于容器化部署

## 安装
### 使用 Docker 部署
1. 构建 Docker 镜像：

```shell
docker build -t omni-flow .
```

2. 运行 Docker 容器：

```shell
docker run -d --name omni-flow -p 8080:8080 omni-flow
```

### 本地构建和运行
1. 克隆仓库：

```shell
git clone https://github.com/your-repo/omni-flow.git
cd omni-flow
```

2. 构建项目：
```shell
mvn clean install
```

3. 启动应用程序：
```shell
java -jar target/omni-flow-0.0.1-SNAPSHOT.jar
```

## 使用
### API 文档
项目集成了 OpenAPI 文档，可以通过访问 `http://localhost:8080/swagger-ui.html` 查看和测试 API。

### 示例请求
你可以通过以下命令向服务发送 HTTP 请求：

```shell
curl -X GET http://localhost:8080/api/xxxx
```

### Portal 使用指南
Omni-Flow 提供了一个 Web Portal，用于管理和监控 AI 任务的创建和执行情况。以下是访问和使用 Portal 的步骤：

#### 访问 Portal
1. **启动应用程序**：确保 Omni-Flow 应用程序已成功启动并运行。
2. **打开浏览器**：在浏览器中输入以下 URL 访问 Portal：

```text
http://localhost:8080/page/controlplane.html
```

#### 主要功能模块
Portal 包含以下几个主要功能模块：

1. **Kind Create**
    - **定义 Kind**：通过填写 JSON 格式的定义，创建新的 Kind 任务。
    - **示例按钮**：点击 "Coze Sample" 按钮可加载一个示例定义。
    - **创建按钮**：点击 "Create" 按钮提交定义，创建新的 Kind 任务。

2. **Kind List**
    - **搜索功能**：通过输入 UserId 并点击 "Search" 按钮，查询特定用户的 Kind 任务列表。
    - **任务列表**：以表格形式展示所有 Kind 任务，包含 ID、Phase、Kind 和创建时间等信息。
    - **分页导航**：使用分页按钮浏览不同页面的任务列表。

3. **详情弹窗**
    - **查看详情**：点击任务列表中的某一行，弹出详情窗口，显示该任务的详细信息。
    - **JSON 查看**：点击 "Json" 按钮查看任务的 JSON 定义及状态。
    - **重新生成**：选择不同的生成步骤并通过 "Regenerate" 按钮重新生成任务。

## 贡献
欢迎为 Omni-Flow 做出贡献！请遵循以下步骤：
1. Fork 本仓库。
2. 创建一个新的分支 (`git checkout -b feature/new-feature`)。
3. 提交你的更改 (`git commit -am 'Add some feature'`)。
4. 推送到分支 (`git push origin feature/new-feature`)。
5. 发起 Pull Request。

## 许可证
Omni-Flow 项目采用 MIT 许可证。详情参见 [LICENSE](LICENSE) 文件。

---

感谢您对 Omni-Flow 的关注和支持！如果您有任何问题或建议，请随时联系我们的开发团队。