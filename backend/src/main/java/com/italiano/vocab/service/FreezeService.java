package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.entity.DailyExtract;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.DailyExtractMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 冻结词口径（单一事实来源，各队列统一调用，别在各 Service 散写）：
 * ① 错题词本在册（in_notebook=1，未在错题本点「学会了」）——所有模式冻结，含学习批次抽取；
 * ② 当前学习批次中还没背完（daily_extract.status=0）——只对考的模式冻结（测验/拼写/听写/加练/统计到期数），
 *    学习批次自身仍要展示这些词；标记完成 status=1 即自动解冻（当日另有防撞兜底）。
 * 变位本（in_conj_notebook）不参与冻结。
 */
@Service
@RequiredArgsConstructor
public class FreezeService {

    private final WordProgressMapper progressMapper;
    private final DailyExtractMapper dailyExtractMapper;

    /** 错题词本在册的词（未学会）：所有模式（含学习批次抽取）都不可见 */
    public Set<Long> notebookIds() {
        return toIdSet(progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getInNotebook, true)));
    }

    /** 当前学习批次未背完的词：考的模式不可见（学习批次自身可见） */
    public Set<Long> unfinishedBatchIds() {
        return toIdSet(dailyExtractMapper.selectList(new LambdaQueryWrapper<DailyExtract>()
                .eq(DailyExtract::getStatus, 0)));
    }

    /** 考的模式（测验/拼写/听写/加练/统计到期数）统一冻结集 = 词本在册 ∪ 批次未完成 */
    public Set<Long> frozenFromReviewIds() {
        Set<Long> ids = notebookIds();
        ids.addAll(unfinishedBatchIds());
        return ids;
    }

    private Set<Long> toIdSet(List<? extends Object> rows) {
        Set<Long> ids = new HashSet<>();
        if (rows != null) {
            for (Object row : rows) {
                if (row instanceof WordProgress p) {
                    ids.add(p.getWordId());
                } else if (row instanceof DailyExtract de) {
                    ids.add(de.getWordId());
                }
            }
        }
        return ids;
    }
}
