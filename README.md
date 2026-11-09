# AI 自动对话 (AI Auto Chat)

Minecraft **1.21.11** Fabric 客户端模组。

别人在聊天栏里提到你的游戏 ID，模组会自动用 AI 生成一句回复替你发出去。默认就是一句普通聊天，看起来像你自己在打字。

> 模组创作者：**_mcbjk_**

---

## 一、安装

### 前置条件

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.20.1 |
| Fabric API | 必须安装 |
| Java | ≥ 21 |

### 步骤

1. 装好 Fabric Loader（官方安装器：<https://fabricmc.net/use/installer/>）。
2. 下载 **Fabric API**，放进 `.minecraft/mods/`。
3. 把 `aidialog-1.0.0.jar` 也放进 `.minecraft/mods/`。
4. 启动游戏。日志里出现下面这行就说明加载成功：
   ```
   [AI-AutoChat] AI 自动对话已加载，创作者 _mcbjk_
   ```

> 这是**客户端模组**，只需要装在你自己这边。服务器不用装。

---

## 二、第一次使用：配置 AI 接口

模组本身不带 AI 能力，需要你填一个可用的 API。

### 1. 按 `K` 打开设置界面

游戏里按 **`K`**，或者在「选项 → 控制 → 按键绑定 → AI 自动对话」里改成你顺手的键。

### 2. 选一个厂商

设置界面第一页「厂商与接口」，左栏直接点选：

| 厂商 | 接口地址 | 默认模型 |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1` | gpt-4o-mini |
| DeepSeek 深度求索 | `https://api.deepseek.com/v1` | deepseek-chat |
| Kimi 月之暗面 | `https://api.moonshot.cn/v1` | moonshot-v1-8k |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | glm-4-flash |
| 通义千问 DashScope | `https://dashscope.aliyuncs.com/compatible-mode/v1` | qwen-plus |
| 硅基流动 SiliconFlow | `https://api.siliconflow.cn/v1` | Qwen/Qwen2.5-7B-Instruct |
| 百度文心一言 | `https://qianfan.baidubce.com/v2` | ernie-4.5-turbo-128k |
| MiniMax | `https://api.minimax.chat/v1` | abab6.5s-chat |
| 阶跃星辰 StepFun | `https://api.stepfun.com/v1` | step-2-mini |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta` | gemini-2.0-flash |
| Anthropic Claude | `https://api.anthropic.com/v1` | claude-sonnet-4-20250514 |
| Ollama 本地 | `http://localhost:11434/v1` | llama3.1 |
| LM Studio 本地 | `http://localhost:1234/v1` | local-model |
| **自定义接口** | 自己填 | 自己填 |

左键 = 下一个，右键 = 上一个。选中后接口地址和模型会自动填好，但**仍然可以手动改**。

### 3. 填 API Key

在「API Key」那一栏粘贴你的密钥（`sk-...`）。

- 本地模型（Ollama / LM Studio）通常不需要 Key，留空即可。
- Key 会明文存在 `.minecraft/config/aidialog.json` 里，别把这个文件发给别人。

### 4. 点「测试连接」

结果会直接发到游戏聊天栏：

- 绿色 `测试成功：xxx` → 配置没问题，可以用了。
- 红色 `测试失败：...` → 看提示排查（见文末「常见问题」）。

### 5. 保存并关闭

点右下角「保存并关闭」。配置会自动写入 `config/aidialog.json`。

> **想用自建/中转服务**（one-api、new-api、vLLM 等）：选「自定义接口」，把接口地址填成对应的 `.../v1`，模型名填你服务里的模型 ID 即可。绝大多数这类服务都兼容 OpenAI 格式。

---

## 三、日常使用

### 触发条件

模组在启动和进入世界时会读取你**当前登录的账号 ID**，把它作为触发词。当聊天栏里有**其他玩家**说出这个 ID 时，就会自动回复。

举例，你的账号是 `Player377`：

```
<Tester2> Player377 在吗          → 触发，自动回复
<Tester2> 有人看到 Player377 吗    → 触发
<Tester2> Player3777 你好          → 不触发（整词匹配）
你自己说话                          → 不触发
```

默认规则：

- 自己发的消息永远不触发。
- 英文/数字 ID 默认**整词匹配**，`Player377` 不会命中 `Player3777`。
- 玩家名大小写不敏感（`player377` 也能触发）。
- 同一个玩家有 **15 秒冷却**，防止刷屏。
- 收到后随机等 **1.2 ~ 4 秒**才发出去，模拟真人在打字。

### 想加别的触发词

比如想让别人喊「大佬」也触发：设置界面第二页「触发与回复」→「触发词」栏填 `大佬`（多个词用逗号隔开）。登录名会继续自动生效。

如果只想用自定义词、不想被登录名触发，把「用登录名自动触发」关掉。

### 快捷键

| 默认键 | 作用 |
| --- | --- |
| `K` | 打开设置界面 |
| `O` | 快速开关自动回复（状态会发到聊天栏） |

改键：**选项 → 控制 → 按键绑定 → AI 自动对话**。

设置界面打开时**不会暂停游戏**，在服务器里也能随时调。

---

## 四、常用调整

设置界面第二、三页能改的东西：

| 想达到的效果 | 改哪个 |
| --- | --- |
| 回复太慢/太快 | 「最短/最长回复延迟」 |
| 有人刷屏式喊你 | 调大「每人冷却」 |
| 回复太长被截断 | 调大「回复长度上限」（最大 256） |
| 回复太啰嗦 | 调小「最大 token」，或改人设 |
| 语气不像你 | 第三页改「人设 System Prompt」 |
| 回复偶尔很怪 | 调低「随机性 temperature」 |
| 想看实际发了什么给 AI | 第三页打开「调试日志」，然后看 `logs/latest.log` |
| 想让它别乱加引号/换行 | 保持「输出净化」开启 |

### 关于人设

第三页的人设框里，`{player}` 会自动替换成你的游戏 ID。默认人设：

```
你是一个正在玩 Minecraft 的玩家，名字叫 {player}。你在游戏里随口聊天，说话简短、口语化、
有点幽默，不用书面语，不解释自己是 AI。绝对不要使用换行，不要加任何前缀。
```

想换个风格就把这段改写掉，比如加一句「你是个高冷的大佬，回复不超过 8 个字」。

---

## 五、配置文件

位置：`.minecraft/config/aidialog.json`

懒得开游戏也可以直接改这个文件，重启游戏生效。主要字段：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `triggerKeyword` | `""` | 额外触发词，逗号分隔；留空则只用登录名 |
| `useAccountIdAsTrigger` | `true` | 是否自动用登录名当触发词 |
| `ignoreCase` | `true` | 忽略大小写 |
| `wholeWordOnly` | `true` | 英文/数字触发词整词匹配 |
| `cooldownSeconds` | `15` | 每个玩家的冷却秒数（上限 3600） |
| `replyDelayMinMs` / `replyDelayMaxMs` | `1200` / `4000` | 发送前的随机延迟区间 |
| `baseUrl` | `https://api.deepseek.com/v1` | 接口地址 |
| `apiKey` | `""` | 密钥 |
| `model` | `deepseek-chat` | 模型名 |
| `apiStyle` | `OPENAI` | 请求格式：`OPENAI` / `GEMINI` / `ANTHROPIC` |
| `temperature` | `0.8` | 随机性，0.0 ~ 2.0 |
| `maxTokens` | `120` | 单次生成上限，16 ~ 1024 |
| `timeoutSeconds` | `30` | 请求超时，5 ~ 180 |
| `systemPrompt` | 见上 | 人设 |
| `sendContext` | `true` | 是否把最近聊天一起发给模型 |
| `contextMessageCount` | `8` | 上下文条数，0 ~ 50 |
| `maxReplyLength` | `256` | 回复长度上限，**再大也不会超过聊天栏硬上限 256** |
| `sanitizeOutput` | `true` | 去掉换行、代码块、名字前缀、包裹引号 |
| `logRequests` | `false` | 把完整请求/响应写进日志 |

---

## 六、常见问题

**测试连接失败，提示 `HTTP 401`**
API Key 不对或已欠费。检查 Key 有没有复制全，以及账户余额。

**提示「无法连接服务器」**
接口地址写错了。注意要带 `/v1` 这类后缀，别只填域名。

**提示「请求超时」**
网络到该厂商不稳，或模型太慢。可以在第一页把「请求超时」调大，或换个模型。

**提示「没有填写 API 接口地址」**
选了「自定义接口」但没填地址，或者地址被清空了。

**别人喊我了但没回复**
逐条排查：
1. 总开关是不是被 `O` 键关了？（设置界面顶部会显示「● 运行中 / ● 已暂停」）
2. 他说的是不是你的**完整 ID**？整词匹配下 `Player37` 不会触发 `Player377`。
3. 是不是刚被同一个人触发过？15 秒冷却内不会再回。
4. 是不是你自己发的？自己的消息不会触发。
5. 打开「调试日志」，看 `logs/latest.log` 里有没有 `提到了「xxx」，准备自动回复`。

**回复里带了引号或者名字前缀**
保持「输出净化」开启（默认就是开的）。

**服务器提示我发言过快 / 被反挂机插件踢了**
模组发的是普通聊天，服务端可能把它当成脚本。把「最短/最长回复延迟」调大，或者把冷却调大，降低频率。

**单人存档里怎么试都没反应**
正常。单人世界里除了你自己没有别的玩家，而自己的消息不触发。想测试可以「对局域网开放」，再用第二个客户端连进来。

**换了账号之后要重新配吗？**
不用。触发词跟着登录账号走，进世界时会自动重新识别。

---

## 七、从源码构建（可选）

需要 JDK 21 编译、JDK 25 供 Gradle 运行（Loom 1.19 的要求）。

```bash
./gradlew build       # 产物在 build/libs/aidialog-1.0.0.jar
./gradlew test        # 40 个单元测试
./gradlew runClient   # 启动开发客户端
./gradlew runClient2  # 启动第二个客户端（Tester2），用来联调多人触发
```

`runClient` 和 `runClient2` 用各自独立的运行目录，可以同时开、开局域网互测。

---

## 八、已知限制

- 只能看到服务端已经下发到客户端的聊天。服务器如果屏蔽聊天或改了格式，可能收不到触发词。
- 只响应真人玩家的公开聊天，不响应系统消息和动作栏文字。
- 回复走的是普通聊天，装了反挂机/反机器人插件的服务器可能判定为异常。
- 纯客户端模组，无法绕过服务端对聊天的任何限制。

---

## 许可

见 [LICENSE](LICENSE)。
