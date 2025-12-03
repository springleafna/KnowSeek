package com.springleaf.knowseek.test;

import com.springleaf.knowseek.mq.parser.impl.TxtFileParser;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
public class TxtFileParserTest {

    @Resource
    private TxtFileParser txtFileParser;

    @Test
    public void testTxtFileParserChunkingWithOverlapAndSemanticSplit() throws Exception {
        // 模拟一个包含多个段落、长句、无段落的混合文本
        String text = """
                第一段内容。这是该段的第一句话。第二句话很长，用来测试当句子接近或超过 CHUNK_SIZE 时是否会被合理切分，\
                但因为我们设置了递归分片逻辑，它应该不会被硬切断。第三句话结束本段。

                第二段开始。这是一个较短的段落，仅包含两句话。注意段落之间有空行。

                第三段是非常长的一段，没有内部空行，也没有很多句号，\
                所以它会依赖句子分隔符进行切分。This is a mixed English-Chinese sentence. \
                Another sentence in English. 这是为了测试中英文标点分隔的效果。\
                继续添加更多内容以确保总长度超过 3000 字符，从而触发多次分片逻辑，\
                并验证跨缓冲区的重叠机制是否生效。重复一些句子来增加长度。重复一些句子来增加长度。\
                重复一些句子来增加长度。重复一些句子来增加长度。重复一些句子来增加长度。
                整个流程始于开发者向 GitHub 仓库的 main/master 分支提交代码，自动触发 GitHub Actions 流水线。
                CI 阶段首先在 GitHub 提供的 Ubuntu 虚拟环境中配置 JDK 21，利用 Maven 编译项目并跳过测试生成可执行的 JAR 包。
                进入 CD 阶段后，系统通过 SCP 协议将构建好的 JAR 包、Dockerfile 及 docker-compose.yml 文件安全传输至生产服务器的指定目录。
                紧接着，通过 SSH 远程连接服务器执行部署脚本，Docker Compose 依据配置拉取 OpenJDK 基础镜像，将新 JAR 包打包进容器镜像中，并根据环境变量强制指定日志路径。
                最终，Docker 自动清理旧容器，启动绑定了宿主机日志挂载点的新容器，同时应用 JSON 日志轮转策略，实现了从代码提交到服务更新、日志持久化存储的全自动化交付。\
                
                物理学有一个第一性原理， 指的是根据一些最基本的物理学常量，从头进行物理学的推导，进而得到整个物理学体系。\
                第一性原理就是让我们抓住事物最本质的特征原理，依据事物本身的规律，去推导、分析、演绎事物的各种变化规律，进而洞悉事物在各种具体场景下的表现形式，
                而不是追随事物的表面现象，生搬硬套各种所谓的规矩、经验和技巧，以至于在各种纷繁复杂的冲突和纠结中迷失了方向。
                
                各位评委老师你们好，接下来由我来介绍我们的项目 智论脉动。
                p4-随着高等教育普及，大学生数量持续激增，预计未来还会大幅增长，这也意味着学术创作需求显著提高。然而在传统论文写作过程中，存在着三大核心痛点：
                p5-首先是格式复杂混乱 - 老师发给学生标准模板，但收回来的材料格式不一，反复修改浪费大量时间；
                其次是沟通效率低下 - 师生之间缺乏实时协作机制，进度跟踪困难，反馈不及时；
                最后是答辩经验不足 - 学生缺乏答辩练习机会，临场发挥不佳影响最终成绩。
                
                p7-针对上述核心痛点，我们开发了'智论脉动'系统来有效解决这些问题。
                对于沟通问题，我们构建了师生协作生态，通过师生双向选择绑定指导关系，通过任务发布、邮件通知和专属课题组群聊，以及导师实时评审、精准批注，让师生协作更高效透明。
                对于格式问题，我们提供自定义论文格式模板，通过拖拽组件实现格式预设，使学生只需专注内容创作本身，最后一键导出标准Word文档，彻底解决格式不统一问题，助力用户实现“提笔即写，落笔成规 ”的高效写作模式。
                同时我们还集成了RAG知识库，支持上传参考文献，在写作过程中使用AI智能润色生成与文献相关的高质量内容；并通过数据建模功能导入SQL自动生成ER图和类图，便于在论文写作过程中直接使用。
                对于答辩问题，我们开发了AI模拟答辩功能，通过智能解析学生论文内容生成相关答辩问题，并支持自定义难度调节，结合AI虚拟人技术构建接近真实的答辩场景，帮助学生提前适应答辩环境。
                接下来我们通过视频观看具体的功能演示。
                项目介绍完毕，请各位评委老师批评指正。
                """;

        // 创建输入流
        InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        // 使用有界队列防止 OOM（实际生产中可能是无界或背压）
        BlockingQueue<String> chunkQueue = new ArrayBlockingQueue<>(100);

        // 创建解析器并执行
        txtFileParser.parse(inputStream, chunkQueue);

    }
}
