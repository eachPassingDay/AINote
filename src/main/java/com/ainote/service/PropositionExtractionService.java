package com.ainote.service;

import com.ainote.dto.CodePropositionDTO;
import com.ainote.dto.PropositionDTO;
import com.ainote.entity.NoteChunk;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class PropositionExtractionService {

    private final ChatModel chatModel;

    public PropositionExtractionService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Async("llmExecutor")
    public CompletableFuture<List<PropositionDTO>> extractTextPropositions(NoteChunk chunk) {
        BeanOutputConverter<List<PropositionDTO>> outputConverter = new BeanOutputConverter<>(
                new ParameterizedTypeReference<List<PropositionDTO>>() {
                });

        String formatInstruction = outputConverter.getFormat();

        String systemPromptStr = """
                你是一个顶级的知识架构师，擅长执行‘命题级检索 (Propositional Retrieval)’的数据清洗工作。
                请阅读用户提供的文本，并将其拆解为多个高质量的、自包含的知识命题（Propositions）。

                【绝对红线】：
                指代消解与自包含（最重要）：每个命题必须是一句完整的、毫无歧义的话。如果原文中使用了代词（它、这）或省略了主语，你必须根据上下文将其替换为具体的专有名词。绝对不能出现“表现差”、“指标提升了”这种没有主语的残句！
                逻辑内聚性：不要机械地按标点符号切分。如果几句话描述的是同一个‘原因和结果’或‘问题与解决方案’，请将它们合并为一个完整的命题。
                数据不可篡改：严禁修改原文中的任何数字、专有名词、代码片段。

                【正确提取示例 (Few-Shot)】：
                原文输入：‘我的初始实现采用1024字符固定分块。在表格密集文档上表现差。表格行被切断。后改为基于HTML标签的语义分块。表格完整性指标提升40%%。未来考虑引入递归分块。支持章节级粗检索和段落级细检索的混合策略。以优化不同查询类型的体验。’

                期望的输出结果：
                [
                {
                "concept": "1024字符固定分块的缺陷",
                "proposition": "采用1024字符固定分块的初始实现，在表格密集型文档上表现较差，会导致表格行被强行切断。"
                },
                {
                "concept": "HTML语义分块的优势",
                "proposition": "将分块策略改为基于HTML标签的语义分块后，系统的表格完整性指标提升了40%%。"
                },
                {
                "concept": "混合检索与递归分块规划",
                "proposition": "系统未来计划引入递归分块技术，支持章节级粗检索和段落级细检索的混合策略，以优化不同查询类型的检索体验。"
                }
                ]


                【正确提取示例 2 (Few-Shot)】：
                原文输入：'Redis 的持久化有两种方式。RDB 通过 fork 子进程执行快照，优点是恢复速度快。但可能丢失最后一次快照后的数据。AOF 记录每条写命令，通过 appendfsync 策略控制刷盘频率。everysec 模式最多丢失 1 秒数据。生产环境建议两者结合使用。'

                期望的输出结果：
                [
                {
                "concept": "RDB 持久化的原理与优缺点",
                "proposition": "Redis 的 RDB 持久化通过 fork 子进程执行快照，恢复速度快，但可能丢失最后一次快照之后的数据。"
                },
                {
                "concept": "AOF 持久化与刷盘策略",
                "proposition": "Redis 的 AOF 持久化记录每条写命令，通过 appendfsync 策略控制刷盘频率，其中 everysec 模式最多丢失 1 秒数据。"
                },
                {
                "concept": "生产环境持久化策略建议",
                "proposition": "在生产环境中，建议将 Redis 的 RDB 和 AOF 两种持久化方式结合使用以兼顾恢复速度和数据安全。"
                }
                ]
                请严格按照上述逻辑和 JSON 格式输出结果。
                %s
                """;

        String systemInstruction = String.format(systemPromptStr, formatInstruction);

        String response = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(chunk.getContent()))))
                .getResult().getOutput().getContent();

        return CompletableFuture.completedFuture(outputConverter.convert(response));
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @Async("llmExecutor")
    public CompletableFuture<CodePropositionDTO> extractCodePropositions(NoteChunk chunk) {
        BeanOutputConverter<CodePropositionDTO> outputConverter = new BeanOutputConverter<>(CodePropositionDTO.class);
        String formatInstruction = outputConverter.getFormat();

        String systemPromptStr = """
                你是一个顶级的代码架构分析师。请阅读用户提供的代码片段组合（可能包含上下文说明文本和由```包裹的代码块）。
                请分析这段代码，并提取其核心元数据。

                【绝对红线】：
                1. 语言识别 (language)：识别代码是哪种编程语言（如 Java, Python, SQL）。
                2. 功能总结 (functionality)：用一句完整、独立的话，准确总结这段代码或配置的业务功能。
                3. 核心 API (core_apis)：列出这段代码中使用到的最重要的类名、库名、注解名或方法名。

                【正确提取示例 1 (Few-Shot)】：
                输入代码片段：
                ```java
                @Transactional
                public void transferMoney(String fromId, String toId, BigDecimal amount) {
                    Account from = accountRepository.findById(fromId).orElseThrow();
                    Account to = accountRepository.findById(toId).orElseThrow();
                    from.debit(amount);
                    to.credit(amount);
                    accountRepository.save(from);
                    accountRepository.save(to);
                }
                ```
                期望的输出结果：
                {"language": "Java", "functionality": "使用 Spring 事务注解实现两个账户之间的转账功能，包含查询、扣款、入账和持久化操作。", "core_apis": ["@Transactional", "JpaRepository.findById", "JpaRepository.save", "BigDecimal"]}

                【正确提取示例 2 (Few-Shot)】：
                输入代码片段：
                ```python
                import pandas as pd
                df = pd.read_csv('sales.csv')
                monthly = df.groupby(df['date'].dt.month)['revenue'].sum()
                monthly.plot(kind='bar', title='Monthly Revenue')
                ```
                期望的输出结果：
                {"language": "Python", "functionality": "使用 Pandas 读取销售数据 CSV 文件，按月份汇总营收并绘制柱状图。", "core_apis": ["pandas.read_csv", "DataFrame.groupby", "Series.plot"]}

                请严格按照期望的 JSON 格式输出结果。
                %s
                """;

        String systemInstruction = String.format(systemPromptStr, formatInstruction);

        String response = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(chunk.getContent()))))
                .getResult().getOutput().getContent();

        return CompletableFuture.completedFuture(outputConverter.convert(response));
    }
}
