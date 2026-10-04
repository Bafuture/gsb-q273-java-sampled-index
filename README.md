# 采样索引构建与误差评估

Pair-wise GSB 标注任务仓库（第 17 批 / 273）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java, Maven, JUnit 5 |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

> 本仓库是**初始环境快照**：只有工程骨架，不含任何实现代码。
> 分支说明：`main` 为初始环境；`A`、`B` 为两次独立执行各自的工作分支，均从 `main` 的同一个提交拉出。

## 运行方式

```bash
./mvnw -q verify
```

## 组件说明

实现位于 `src/main/java/com/example/gsb/sampledindex/`：

| 类 | 职责 |
|----|------|
| `SampledIndexService` | 采样索引服务：抽样建索引、查询标记、动态重建、统计 |
| `RandomSampler` / `StratifiedSampler` | 随机抽样 / 分层抽样（每层至少 1 条，保证稀有类别有样本） |
| `InvertedIndex` | 不可变倒排索引（key -> 记录集），可对样本或全量构建 |
| `QueryResult` | 查询结果，采样索引结果标记 `possiblyIncomplete` 并附说明 |
| `ErrorEvaluator` / `ErrorReport` | 采样索引 vs 全量索引对比，输出命中率、漏检率与误差分布 |
| `RatioErrorAnalyzer` / `RatioErrorPoint` | 多采样比例下的漏检率数据，用于比例-误差关系分析 |
| `IndexStats` | 统计快照：样本量、索引大小、平均漏检率、重建次数 |

### 采样索引何时能用 / 不能用

- **能用**：趋势分析、比例估计、聚合统计、探索性查询、可容忍假阴性的存在性判断。
- **不能用**：精确计数、合规审计、账单结算等要求完整结果集的场景；结果触发不可逆动作的场景；查询集中于稀有类别却使用简单随机抽样的场景（应改用分层抽样）。

### 分层键选择的影响

- 分层键应与查询过滤维度相关：查询常按某维度过滤时，按该维度分层能显著降低漏检率；
- 分层键基数过高（接近每行一个值）时，"每层至少 1 条"的保证会使实际采样率远超配置比例，失去降本意义；
- 分层键与数据分布无关时，效果退化为随机抽样并多付分组开销。

### 动态重建

`SampledIndexService.rebuild(newData, newRatio)` 先在后台构建新索引，完成后通过 `AtomicReference` 原子切换；重建期间旧索引持续服务查询，不中断。`resample(newRatio)` 用于数据分布未变、仅调整比例的场景。

### 典型用法

```java
List<DataRecord> data = ...;
SampledIndexService service = new SampledIndexService(new StratifiedSampler(), data, 0.1, 42L);

QueryResult result = service.query("some-key");
if (result.possiblyIncomplete()) { /* 结果可能不完整，见 result.explanation() */ }

InvertedIndex full = InvertedIndex.build(data, 1.0, "full");
ErrorReport report = service.evaluate(full, List.of("some-key", "other-key"));
double missRate = report.missRate();

List<RatioErrorPoint> curve = new RatioErrorAnalyzer().analyze(
        data, queries, List.of(0.05, 0.1, 0.25, 0.5, 1.0), new RandomSampler(), 42L);

service.rebuild(newData, 0.2);   // 分布变化后重建，期间旧索引仍可用
IndexStats stats = service.stats();
```

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的数据量太大，为全量数据建索引代价高，希望只对样本建索引并给出可用性评估。请从零实现一个采样索引构建与误差评估组件。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持从全量数据中抽样建索引（随机抽样或分层抽样），采样比例可配置；2) 支持用采样索引回答查询并把结果标记为"可能不完整"，需说明何时能用何时不能用；3) 支持误差评估：对同一批查询同时用采样索引与全量索引执行，统计命中率、漏检率与误差分布；4) 支持采样比例与误差的关系分析，给出不同比例下的漏检率数据；5) 支持动态调整采样比例：数据分布变化时能重建索引，重建期间旧索引仍可用；6) 支持分层抽样：按某个维度分层，保证稀有类别也有样本，需说明分层键选择的影响；7) 提供统计：样本量、索引大小、平均漏检率与重建次数；8) 测试覆盖抽样建索引、查询标记、误差评估、比例与误差关系、动态重建与分层抽样；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。
