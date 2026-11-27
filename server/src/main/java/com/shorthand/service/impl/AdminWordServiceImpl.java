package com.shorthand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorthand.common.BizException;
import com.shorthand.common.PageResult;
import com.shorthand.common.WordTextUtils;
import com.shorthand.domain.Word;
import com.shorthand.domain.WordAudio;
import com.shorthand.domain.bo.WordSaveBo;
import com.shorthand.domain.vo.AdminWordVo;
import com.shorthand.mapper.WordAudioMapper;
import com.shorthand.mapper.WordMapper;
import com.shorthand.service.IAdminWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminWordServiceImpl implements IAdminWordService {
    private final WordMapper wordMapper;
    private final WordAudioMapper wordAudioMapper;

    @Value("${app.audio-dir:./data/audio}")
    private String audioDir;

    @Override
    public PageResult list(long pageNo, long pageSize, String keyword) {
        Page<Word> page = wordMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Word>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Word::getWord, keyword)
                                .or()
                                .like(Word::getMeaning, keyword))
                        .orderByDesc(Word::getId));
        List<Long> ids = page.getRecords().stream().map(Word::getId).toList();
        Map<Long, String> audio = ids.isEmpty() ? Map.of() : wordAudioMapper.selectList(
                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<WordAudio>lambdaQuery()
                                .in(WordAudio::getWordId, ids)
                                .eq(WordAudio::getStatus, 2)
                                .eq(WordAudio::getAccent, "us")
                                .eq(WordAudio::getVoice, "af_heart"))
                .stream().collect(Collectors.toMap(WordAudio::getWordId, a -> "/audio/" + a.getObjectKey(), (a, b) -> a));
        return PageResult.of(page.getRecords().stream().map(w -> toVo(w, audio.get(w.getId()))).toList(),
                page.getTotal(), pageNo, pageSize);
    }

    @Override
    public Long create(WordSaveBo bo) {
        String normalized = WordTextUtils.normalizeWord(bo.getWord());
        String headword = WordTextUtils.headwordKey(bo.getWord());
        String pronunciation = WordTextUtils.pronunciationKey(bo.getPhoneticUs(), bo.getPhoneticUk());
        Long count = wordMapper.selectCount(Wrappers.<Word>lambdaQuery()
                .eq(Word::getNormalized, normalized)
                .eq(Word::getHeadwordKey, headword)
                .eq(Word::getPronunciationKey, pronunciation));
        if (count != null && count > 0) throw new BizException("单词已存在");
        Word word = new Word();
        apply(word, bo);
        word.setNormalized(normalized);
        word.setHeadwordKey(headword);
        word.setPronunciationKey(pronunciation);
        wordMapper.insert(word);
        return word.getId();
    }

    @Override
    public void update(Long id, WordSaveBo bo) {
        Word word = wordMapper.selectById(id);
        if (word == null) throw new BizException("词条不存在");
        String normalized = WordTextUtils.normalizeWord(bo.getWord());
        String headword = WordTextUtils.headwordKey(bo.getWord());
        String pronunciation = WordTextUtils.pronunciationKey(bo.getPhoneticUs(), bo.getPhoneticUk());
        Long count = wordMapper.selectCount(Wrappers.<Word>lambdaQuery()
                .eq(Word::getNormalized, normalized)
                .eq(Word::getHeadwordKey, headword)
                .eq(Word::getPronunciationKey, pronunciation)
                .ne(Word::getId, id));
        if (count != null && count > 0) throw new BizException("单词已存在");
        apply(word, bo);
        word.setNormalized(normalized);
        word.setHeadwordKey(headword);
        word.setPronunciationKey(pronunciation);
        wordMapper.updateById(word);
    }

    @Override
    public void delete(Long id) {
        wordMapper.deleteById(id);
    }

    @Override
    public String uploadAudio(Long id, MultipartFile file) {
        Word word = wordMapper.selectById(id);
        if (word == null) throw new BizException("词条不存在");
        if (file == null || file.isEmpty()) throw new BizException("请选择音频文件");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String extension;
        if (name.endsWith(".mp3")) extension = ".mp3";
        else if (name.endsWith(".wav")) extension = ".wav";
        else throw new BizException("只支持 MP3 或 WAV 音频");
        if (file.getSize() > 5 * 1024 * 1024) throw new BizException("音频不能超过 5MB");
        try {
            Path root = Paths.get(audioDir).toAbsolutePath().normalize();
            Path dir = root.resolve("manual");
            Files.createDirectories(dir);
            String objectKey = "manual/" + id + "-" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = root.resolve(objectKey).normalize();
            if (!target.startsWith(root)) throw new BizException("非法音频路径");
            file.transferTo(target.toFile());
            byte[] bytes = Files.readAllBytes(target);
            StringBuilder hash = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hash.append(String.format("%02x", b));

            WordAudio audio = wordAudioMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<WordAudio>lambdaQuery()
                    .eq(WordAudio::getWordId, id)
                    .eq(WordAudio::getAccent, "us")
                    .eq(WordAudio::getVoice, "af_heart")
                    .last("LIMIT 1"));
            String oldKey = audio == null ? null : audio.getObjectKey();
            if (audio == null) {
                audio = new WordAudio();
                audio.setWordId(id);
                audio.setAccent("us");
                audio.setVoice("af_heart");
            }
            audio.setObjectKey(objectKey);
            audio.setChecksum(hash.toString());
            audio.setSizeBytes((int) file.getSize());
            audio.setModel("manual");
            audio.setModelVersion("custom");
            audio.setStatus(2);
            if (audio.getId() == null) wordAudioMapper.insert(audio);
            else wordAudioMapper.updateById(audio);
            if (oldKey != null && !oldKey.equals(objectKey)) {
                Path old = root.resolve(oldKey).normalize();
                if (old.startsWith(root)) Files.deleteIfExists(old);
            }
            return "/audio/" + objectKey;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("音频保存失败：" + e.getMessage());
        }
    }

    private void apply(Word word, WordSaveBo bo) {
        word.setWord(bo.getWord().trim());
        word.setPhoneticUk(bo.getPhoneticUk());
        word.setPhoneticUs(bo.getPhoneticUs());
        word.setPos(bo.getPos());
        word.setMeaning(bo.getMeaning());
        word.setExampleEn(bo.getExampleEn());
        word.setExampleCn(bo.getExampleCn());
    }

    private AdminWordVo toVo(Word word, String audioUrl) {
        AdminWordVo vo = new AdminWordVo();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setNormalized(word.getNormalized());
        vo.setPhoneticUk(word.getPhoneticUk());
        vo.setPhoneticUs(word.getPhoneticUs());
        vo.setPos(word.getPos());
        vo.setMeaning(word.getMeaning());
        vo.setExampleEn(word.getExampleEn());
        vo.setExampleCn(word.getExampleCn());
        vo.setUpdateTime(word.getUpdateTime());
        vo.setAudioUrl(audioUrl);
        return vo;
    }
}
