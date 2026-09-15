package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.italiano.vocab.dto.TodayWordDTO;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题本：测验「不认识」/ 拼写判错的词自动进本，用户背熟后手动移出。
 * 纯标记（in_notebook）零记录——不存错误时间/来源/错次；与三套复习体系零耦合：
 * 进本出本不影响 box / spell_box / extract_count，SRS 回流照常。
 */
@Service
@RequiredArgsConstructor
public class NotebookService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;

    /**
     * 本内词列表（按进本先后稳定排序，id 升序；与学习模式一样位置固定，不随机打乱——
     * 错题本是"翻账本"场景，稳定顺序便于对照回忆，不需要防位置记忆）
     */
    public Map<String, Object> getWords() {
        List<WordProgress> list = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getInNotebook, true)
                .orderByAsc(WordProgress::getId));

        List<TodayWordDTO> words = new ArrayList<>();
        if (!list.isEmpty()) {
            Map<Long, Word> wordById = wordMapper.selectBatchIds(list.stream()
                            .map(WordProgress::getWordId).toList()).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            for (WordProgress p : list) {
                Word w = wordById.get(p.getWordId());
                if (w == null) {
                    continue;
                }
                TodayWordDTO dto = new TodayWordDTO();
                dto.setWordId(w.getId());
                dto.setWord(w.getWord());
                dto.setPos(w.getPos());
                dto.setMeaning(w.getMeaning());
                dto.setCategory(w.getCategory());
                dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
                dto.setExtractCount(p.getExtractCount());
                words.add(dto);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", words.size());
        result.put("words", words);
        return result;
    }

    /** 学会了：移出错题本 */
    @Transactional
    public void learn(Long wordId) {
        setNotebook(wordId, false);
    }

    /** 撤销学会：放回错题本（防手滑） */
    @Transactional
    public void undo(Long wordId) {
        setNotebook(wordId, true);
    }

    /** 全部学会：清空错题本，返回清掉的词数 */
    @Transactional
    public int learnAll() {
        return progressMapper.update(null, new LambdaUpdateWrapper<WordProgress>()
                .eq(WordProgress::getInNotebook, true)
                .set(WordProgress::getInNotebook, false));
    }

    private void setNotebook(Long wordId, boolean inNotebook) {
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, wordId));
        if (p == null) {
            throw new IllegalArgumentException("该单词没有学习记录");
        }
        p.setInNotebook(inNotebook);
        progressMapper.updateById(p);
    }
}
