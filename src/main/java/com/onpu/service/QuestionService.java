package com.onpu.service;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.onpu.jooq.Tables.ANSWER_LOGS;

@Service
public class QuestionService {

    private final DSLContext dsl;
    private final Random random = new Random();

    public QuestionService(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * 苦手な問題を重み付きで選んで返す。
     * 正答率が低い問題ほど高い確率で出題される。
     */
    public String getWeightedQuestion(Long userId, String mode, List<String> allQuestions) {
        // ユーザーの問題別正答率を取得
        Map<String, Double> rateMap = new HashMap<>();
        var results = dsl.select(
                ANSWER_LOGS.QUESTION,
                DSL.count().as("answers"),
                DSL.sum(ANSWER_LOGS.IS_CORRECT.cast(Integer.class)).as("correct")
            )
            .from(ANSWER_LOGS)
            .where(ANSWER_LOGS.USER_ID.eq(userId))
            .and(ANSWER_LOGS.MODE.eq(mode))
            .groupBy(ANSWER_LOGS.QUESTION)
            .fetch();

        for (var r : results) {
            int answers = r.get("answers", Integer.class);
            int correct = r.get("correct", Integer.class);
            if (answers >= 3) {
                rateMap.put(r.get(ANSWER_LOGS.QUESTION), (double) correct / answers);
            }
        }

        // 重み計算: 正答率が低いほど重みが大きい。未回答は中間の重み。
        double[] weights = new double[allQuestions.size()];
        for (int i = 0; i < allQuestions.size(); i++) {
            String q = allQuestions.get(i);
            if (rateMap.containsKey(q)) {
                // 正答率0% → 重み3.0、正答率100% → 重み0.5
                weights[i] = 3.0 - 2.5 * rateMap.get(q);
            } else {
                weights[i] = 1.0; // 未回答 or 回答数不足
            }
        }

        // 重み付きランダム選択
        double totalWeight = Arrays.stream(weights).sum();
        double r = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < allQuestions.size(); i++) {
            cumulative += weights[i];
            if (r <= cumulative) {
                return allQuestions.get(i);
            }
        }
        return allQuestions.get(allQuestions.size() - 1);
    }
}
