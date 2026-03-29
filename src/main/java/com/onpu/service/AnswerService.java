package com.onpu.service;

import com.onpu.dto.AnswerLogResponse;
import com.onpu.dto.AnswerRequest;
import com.onpu.dto.StatsResponse;
import com.onpu.dto.StatsResponse.ModeStats;
import com.onpu.dto.WeakPointResponse;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.onpu.jooq.Tables.ANSWER_LOGS;

@Service
public class AnswerService {

    private final DSLContext dsl;

    public AnswerService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void recordAnswer(Long userId, AnswerRequest request) {
        dsl.insertInto(ANSWER_LOGS)
            .set(ANSWER_LOGS.USER_ID, userId)
            .set(ANSWER_LOGS.MODE, request.mode())
            .set(ANSWER_LOGS.QUESTION, request.question())
            .set(ANSWER_LOGS.IS_CORRECT, request.isCorrect())
            .set(ANSWER_LOGS.ANSWERED_AT, LocalDateTime.now())
            .execute();
    }

    public StatsResponse getStats(Long userId) {
        var results = dsl.select(
                ANSWER_LOGS.MODE,
                DSL.count().as("answers"),
                DSL.sum(ANSWER_LOGS.IS_CORRECT.cast(Integer.class)).as("correct")
            )
            .from(ANSWER_LOGS)
            .where(ANSWER_LOGS.USER_ID.eq(userId))
            .groupBy(ANSWER_LOGS.MODE)
            .fetch();

        int totalAnswers = 0;
        int totalCorrect = 0;
        Map<String, ModeStats> byMode = new HashMap<>();

        for (var r : results) {
            String mode = r.get(ANSWER_LOGS.MODE);
            int answers = r.get("answers", Integer.class);
            int correct = r.get("correct", Integer.class);
            totalAnswers += answers;
            totalCorrect += correct;
            byMode.put(mode, new ModeStats(answers, correct));
        }

        return new StatsResponse(totalAnswers, totalCorrect, byMode);
    }

    public List<AnswerLogResponse> getHistory(Long userId, int limit) {
        return dsl.selectFrom(ANSWER_LOGS)
            .where(ANSWER_LOGS.USER_ID.eq(userId))
            .orderBy(ANSWER_LOGS.ANSWERED_AT.desc())
            .limit(limit)
            .fetch(r -> new AnswerLogResponse(
                r.getMode(),
                r.getQuestion(),
                r.getIsCorrect(),
                r.getAnsweredAt()
            ));
    }

    public List<WeakPointResponse> getWeakPoints(Long userId, String mode, int limit) {
        return dsl.select(
                ANSWER_LOGS.QUESTION,
                DSL.count().as("answers"),
                DSL.sum(ANSWER_LOGS.IS_CORRECT.cast(Integer.class)).as("correct")
            )
            .from(ANSWER_LOGS)
            .where(ANSWER_LOGS.USER_ID.eq(userId))
            .and(ANSWER_LOGS.MODE.eq(mode))
            .groupBy(ANSWER_LOGS.QUESTION)
            .having(DSL.count().ge(3))
            .orderBy(DSL.sum(ANSWER_LOGS.IS_CORRECT.cast(Integer.class)).cast(Double.class).div(DSL.count()).asc())
            .limit(limit)
            .fetch(r -> {
                int answers = r.get("answers", Integer.class);
                int correct = r.get("correct", Integer.class);
                return new WeakPointResponse(
                    r.get(ANSWER_LOGS.QUESTION),
                    answers,
                    correct,
                    answers > 0 ? (double) correct / answers : 0
                );
            });
    }
}
