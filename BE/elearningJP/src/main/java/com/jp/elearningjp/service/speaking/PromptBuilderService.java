package com.jp.elearningjp.service.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.entity.speaking.AiMessage;
import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.entity.speaking.AiSession;
import com.jp.elearningjp.shared.enums.AiPracticeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service xây dựng System Instruction và ngữ cảnh hội thoại cho LLM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilderService {

    private final ObjectMapper objectMapper;

    /**
     * Xây dựng LlmSpeakingPrompt chuẩn xác từ phiên làm việc và lịch sử tin nhắn.
     */
    public LlmSpeakingPrompt buildPrompt(AiSession session, List<AiMessage> recentHistory, String userNewMessage) {
        String systemInstruction;

        if (session.getMode() == AiPracticeMode.SCENARIO && session.getScenario() != null) {
            systemInstruction = buildScenarioInstruction(session.getScenario(), session.getCompletedTasks());
        } else {
            systemInstruction = buildFreeChatInstruction(session.getTitle());
        }

        return LlmSpeakingPrompt.builder()
                .systemInstruction(systemInstruction)
                .recentHistory(recentHistory != null ? recentHistory : new ArrayList<>())
                .userNewMessage(userNewMessage)
                .build();
    }

    private String buildScenarioInstruction(AiScenario scenario, JsonNode completedTasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI Japanese Speaking Partner roleplaying in a JLPT practice app.\n");
        sb.append("### SITUATION CONTEXT:\n");
        sb.append("- Topic: ").append(scenario.getTopicId()).append("\n");
        sb.append("- JLPT Level: ").append(scenario.getJlptLevel()).append("\n");
        sb.append("- Scenario Title: ").append(scenario.getTitle()).append("\n");
        sb.append("- Description: ").append(scenario.getDescription()).append("\n");
        sb.append("- Your Role (AI): ").append(scenario.getAiRole()).append("\n");
        sb.append("- Learner Role (User): ").append(scenario.getUserRole()).append("\n\n");

        sb.append("### TASKS TO COMPLETE:\n");
        if (scenario.getTasks() != null && scenario.getTasks().isArray()) {
            for (JsonNode task : scenario.getTasks()) {
                int taskId = task.path("id").asInt();
                String taskText = task.path("text").asText();
                boolean isCompleted = false;
                if (completedTasks != null && completedTasks.isArray()) {
                    for (JsonNode completedId : completedTasks) {
                        if (completedId.asInt() == taskId) {
                            isCompleted = true;
                            break;
                        }
                    }
                }
                sb.append("  * [Task ID ").append(taskId).append("]: ").append(taskText)
                  .append(isCompleted ? " (ALREADY COMPLETED)" : " (NOT COMPLETED YET)")
                  .append("\n");
            }
        }
        sb.append("\n");

        if (scenario.getVocabulary() != null && !scenario.getVocabulary().isEmpty()) {
            sb.append("### RECOMMENDED VOCABULARY: ").append(scenario.getVocabulary().toString()).append("\n");
        }
        if (scenario.getGrammar() != null && !scenario.getGrammar().isEmpty()) {
            sb.append("### RECOMMENDED GRAMMAR: ").append(scenario.getGrammar().toString()).append("\n");
        }

        appendCoreRules(sb, scenario.getJlptLevel().name());
        return sb.toString();
    }

    private String buildFreeChatInstruction(String topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are 'Maziiko' (マジーコ), a friendly, patient, and encouraging Japanese friend.\n");
        sb.append("### FREE TALK CONTEXT:\n");
        sb.append("- Topic: ").append(topic != null ? topic : "日常会話 (Daily life & Hobbies)").append("\n");
        sb.append("- Your Persona: Warm Japanese friend who loves chatting, asks engaging questions, and helps the learner speak naturally.\n\n");

        appendCoreRules(sb, "N4~N3");
        return sb.toString();
    }

    private void appendCoreRules(StringBuilder sb, String level) {
        sb.append("""
        ### STRICT RULES FOR YOUR RESPONSE:
        1. LENGTH: Keep your response short: strictly 1 to 2 sentences in natural Japanese. Do not lecture. Short sentences make listening and speaking practice conversational.
        2. LEVEL APPROPRIATE: Restrict your vocabulary and grammar strictly within JLPT level %s. Do not use overly complex or rare kanji/idioms.
        3. NATURAL FLOW: Keep the roleplay and conversation going naturally. If the user makes an error, understand their intent, reply naturally without breaking character.
        4. OFF-TOPIC HANDLING: If the user speaks off-topic, gently guide them back to the situation.
        5. CORRECTION:
           - If the user made a grammar, vocabulary, or particle mistake in their last sentence:
             Fill the "correction" object with:
               "original": the exact faulty phrase the user said
               "fixed": the natural, correct Japanese phrasing
               "note_vi": concise explanation in Vietnamese explaining the mistake (e.g. "Dùng trợ từ に thay vì を khi chỉ điểm đến")
           - If the user spoke completely correctly or just greeted: set "correction" to null.
        6. TASK EVALUATION:
           - In the "completed_task_ids" list, include the integer IDs of all tasks that have been fulfilled up to this turn (including previously completed ones and newly completed ones).
        7. OUTPUT FORMAT:
           - You MUST return a single valid JSON object strictly matching this schema:
           {
             "reply_ja": "短い日本語の返事（1〜2文）",
             "reply_vi": "Bản dịch nghĩa tiếng Việt tự nhiên và chuẩn xác của câu reply_ja ở trên",
             "correction": {
               "original": "ユーザーの間違えた部分",
               "fixed": "自然な正しい表現",
               "note_vi": "Giải thích ngắn bằng tiếng Việt"
             },
             "completed_task_ids": [0, 1]
           }
           - If no correction is needed, "correction" MUST be null.
           - DO NOT wrap the output in markdown codeblocks like ```json. Return raw JSON string only.
        """.formatted(level));
    }
}
