package org.mf.treehole.util;


import org.mf.treehole.common.DeepSeekResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeepSeekChat {
    private static final Map<String, Object> REQUEST_BODY = new LinkedHashMap<>();

    private DeepSeekChat() {}

    static {
        REQUEST_BODY.put("model", "deepseek-v4-flash");
        REQUEST_BODY.put("stream", false);
        REQUEST_BODY.put("max_tokens", 4096);

        Map<String, Object> thinking = new LinkedHashMap<>();
        thinking.put("type", "disabled");
        REQUEST_BODY.put("thinking", thinking);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", ""));
        messages.add(Map.of("role", "user", "content", ""));
        REQUEST_BODY.put("messages", messages);
    }

    public static void asyncChat(String systemPrompt, String userPrompt, WhenGetResponse get) {
        Thread thread = new Thread(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + System.getenv("DEEPSEEK_API_KEY"))
                    .POST(HttpRequest.BodyPublishers.ofString(getRequestBody(systemPrompt, userPrompt)))
                    .build();
            try(HttpClient httpClient = HttpClient.newHttpClient()) {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    get.run(new ObjectMapper().readValue(response.body(), DeepSeekResponseBody.class));
                } else {
                    System.err.println(response.body());
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(System.err);
                get.run(DeepSeekResponseBody.err());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void TreeholeRobot(String userPrompt, WhenGetResponse get) {
        asyncChat("""
                ## 1. 角色与核心目标
                你是一个温暖、善解人意的校园树洞机器人，名叫“小树灵”。你的核心目标是认真倾听同学的烦恼，提供情感支持和安慰，而不是解决问题或给出建议。
                
                ## 2. 行为边界与准则
                - **必须做**:
                    - 首先表达**共情和理解**，认可用户的情绪是合理的。
                    - 使用温和、平静、非评判性的语气，像朋友一样交流。
                    - 积极倾听，可以从用户的倾诉中提取关键词进行回应。
                - **绝对禁止**:
                    - **不诊断、不治疗**：不提供任何心理健康诊断或医疗建议。
                    - **不灌输鸡汤**：避免说教和使用陈词滥调的安慰，例如“一切都会好的”，这反而会显得敷衍和无效。
                    - **不主动提供解决方案**：除非用户明确询问，否则不要提供解决方案。
                
                ## 3. 回复的结构化流程(思维链)
                在生成每次回复前，请遵循以下思考步骤：
                1.  **情绪识别**：分析用户倾诉中传达的核心情绪是什么？（如：孤独、焦虑、委屈）
                2.  **事实归纳**：用户遇到了一个什么样的事件或情境？
                3.  **共情回应**：基于识别出的情绪，先给予情感上的认可和接纳。
                4.  **避免追问**：除非你未能理解用户的问题，否则不要追问，根据已有信息表达安慰。
                5.  **安全检查**：如果用户的倾诉涉及自伤、自残或伤害他人的内容，**必须立即停止安慰，并建议其联系学校心理咨询中心或寻求身边人的帮助**。
                
                ## 4. 输出格式要求
                - 回复应自然、口语化，像一封简短的信或朋友的留言。
                - 字数控制在100-200字，避免长篇大论。
                - 可以使用颜文字或温和的emoji来缓和气氛，但不要滥用。
                
                ## 5. 不确定时的备用策略
                如果用户的问题你无法理解，或者信息不足以让你共情，请温和地承认，并引导用户更多表达：
                “嗯…我可能还没完全理解你的感受，如果你愿意，可以多说一点吗？我会在这里听着。”
                """, userPrompt, get);
    }

    public interface WhenGetResponse {
        void run(DeepSeekResponseBody responseBody);
    }

    private static String getRequestBody(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        REQUEST_BODY.put("messages", messages);
        return new ObjectMapper().writeValueAsString(REQUEST_BODY);
    }
}
