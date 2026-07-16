package org.mf.treehole.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeepSeekResponseBody {
    private String id;
    private List<Choice> choices;
    private Long created;
    private String model;
    private String object;
    private Usage usage;

    public String getResult() {
        if (id.equals("error")) {
            return "error";
        }
        if (choices != null && !choices.isEmpty()) {
            Choice first = choices.getFirst();
            if (first.getMessage() != null) {
                return first.getMessage().getContent();
            }
        }
        return "null";
    }

    public static DeepSeekResponseBody err() {
        return new DeepSeekResponseBody(
                "error",
                null,
                -1L,
                "null",
                "null",
                null
        );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Choice {
        private String finish_reason;
        private Integer index;
        private Message message;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Message {
            private String content;
            private String role;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Usage {
        private Integer completion_tokens;
        private Integer prompt_tokens;
        private Integer total_tokens;
    }
}
