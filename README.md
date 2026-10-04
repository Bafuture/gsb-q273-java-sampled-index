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

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的数据量太大，为全量数据建索引代价高，希望只对样本建索引并给出可用性评估。请从零实现一个采样索引构建与误差评估组件。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持从全量数据中抽样建索引（随机抽样或分层抽样），采样比例可配置；2) 支持用采样索引回答查询并把结果标记为"可能不完整"，需说明何时能用何时不能用；3) 支持误差评估：对同一批查询同时用采样索引与全量索引执行，统计命中率、漏检率与误差分布；4) 支持采样比例与误差的关系分析，给出不同比例下的漏检率数据；5) 支持动态调整采样比例：数据分布变化时能重建索引，重建期间旧索引仍可用；6) 支持分层抽样：按某个维度分层，保证稀有类别也有样本，需说明分层键选择的影响；7) 提供统计：样本量、索引大小、平均漏检率与重建次数；8) 测试覆盖抽样建索引、查询标记、误差评估、比例与误差关系、动态重建与分层抽样；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。

---

## 实现说明：采样索引构建与误差评估组件

代码位于 `com.example.gsb.sampledindex` 包，无任何第三方运行时依赖。

### 核心类

| 类 | 职责 |
|----|------|
| `Document` | 数据记录：id + 文本 + 属性维度（分层键来源） |
| `InvertedIndex` | 内存倒排索引（AND 语义），全量索引与采样索引共用 |
| `RandomSampler` | 均匀随机抽样，比例 `[0,1]` 可配，确定性哈希保证可复现与样本嵌套 |
| `StratifiedSampler` | 按维度分层抽样，每层保底 `minPerStratum` 条，稀有类别不丢失 |
| `SampledIndex` | 基于样本建索引，查询结果恒标记 `possiblyIncomplete=true` |
| `QueryResult` | 查询结果 + “可能不完整”标记 |
| `ErrorEvaluator` / `ErrorReport` | 同批查询双跑（采样 vs 全量），命中率/漏检率/误差分布 |
| `RatioErrorAnalyzer` | 多采样比例扫描，输出比例-漏检率曲线 |
| `SampledIndexService` | 持有当前索引，支持同步/异步重建、构建期旧索引可用、运行统计 |
| `IndexStats` | 样本量、索引大小（posting 数）、平均漏检率、重建次数 |

### 何时能用 / 何时不能用

采样索引的结果**一定是全量结果的子集**（无假阳性，只有漏检）。

- **能用**：探索性分析、结果预览、Top-K 的初筛召回、可容忍近似的统计/计数、在误差评估中已确认平均漏检率低于业务阈值的固定查询集。
- **不能用**：要求结果精确/穷尽的场景（合规审计、精确计数、去重总数）、对召回率敏感的低频词/稀有词查询（样本可能完全漏掉）、分页深翻页（漏检使总数与偏移失效）、需要“确认不存在”的查询（采样索引的空结果只代表样本里没有）。
- **硬保证**：若某查询必须完整，可对该查询回退全量索引，或把采样比例设为 `1.0`。

### 采样比例与误差

- 随机抽样下，单条文档漏检概率 ≈ `1 - ratio`，查询命中文档数的期望召回为 `ratio`；实际漏检率随数据与查询而波动，应以 `RatioErrorAnalyzer` 在真实查询集上实测。
- 比例-漏检率数据由 `RatioErrorAnalyzer.analyze(data, queries, List.of(0.05, 0.1, 0.25, 0.5, 1.0), seed)` 产出，选“可接受漏检率下最省索引成本”的比例。
- `RandomSampler` 用 id 的确定性哈希入选阈值，样本随比例嵌套（ratio 小的样本 ⊆ ratio 大的样本），因此漏检率曲线单调不增，且重建结果可复现。

### 动态重建

`SampledIndexService.rebuildAsync(newData, sampler)` 在后台完整构建新索引，
完成后通过 `AtomicReference` 原子替换；构建期间 `query()` 始终由旧索引回答，无空窗。
数据分布变化（如新增类别）时，用新的分层键或比例重建即可。

### 分层键选择的影响

- 选择与**查询相关性强**的维度（如商品类目、地区）：保证每个业务关心的类别都有样本，稀有类别查询不会整类漏检。
- 键的基数要适中：基数过高（如按用户 id 分层）会使层数过多、每层保底条数推高实际样本量，偏离配置比例。
- 与查询无关的键（如纯技术字段）只能提供均匀分布保证，收益有限。
- 缺失键值的文档统一归入 `<missing>` 层。
- 保底机制使实际比例可能高于配置比例（`SampledIndex.effectiveSampleRatio()` 可查实际值）。

### 用法示例

```java
SampledIndexService service = new SampledIndexService();
service.rebuild(allDocuments, new RandomSampler(0.1, 42L));          // 10% 随机抽样
// service.rebuild(allDocuments,
//     new StratifiedSampler(0.1, "category", 2, 42L));              // 或按类目分层

QueryResult r = service.query(List.of("error", "timeout"));
boolean maybeIncomplete = r.possiblyIncomplete();                    // 恒为 true

InvertedIndex full = InvertedIndex.build(allDocuments);
ErrorReport report = service.evaluateAgainst(queries, full);
report.hitRate();      // 命中率（召回率）
report.missRate();     // 漏检率
report.missCountHistogram(); // 单查询漏检条数分布

List<RatioErrorPoint> curve = new RatioErrorAnalyzer()
        .analyze(allDocuments, queries, List.of(0.05, 0.1, 0.25, 0.5, 1.0), 42L);

service.stats();       // IndexStats: 样本量 / 索引大小 / 平均漏检率 / 重建次数
```
