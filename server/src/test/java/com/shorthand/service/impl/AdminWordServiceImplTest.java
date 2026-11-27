package com.shorthand.service.impl;

import com.shorthand.domain.Word;
import com.shorthand.domain.WordAudio;
import com.shorthand.mapper.WordAudioMapper;
import com.shorthand.mapper.WordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWordServiceImplTest {

    private WordMapper wordMapper;
    private WordAudioMapper wordAudioMapper;
    private AdminWordServiceImpl service;

    @TempDir
    Path audioDir;

    @BeforeEach
    void setUp() {
        wordMapper = mock(WordMapper.class);
        wordAudioMapper = mock(WordAudioMapper.class);
        service = new AdminWordServiceImpl(wordMapper, wordAudioMapper);
        ReflectionTestUtils.setField(service, "audioDir", audioDir.toString());
    }

    @Test
    void uploadAudioAcceptsWavAndStoresOriginalExtension() throws Exception {
        Word word = new Word();
        word.setId(1L);
        when(wordMapper.selectById(1L)).thenReturn(word);
        when(wordAudioMapper.selectOne(any())).thenReturn(null);

        // Minimal PCM WAV header + one sample; upload path does not decode it.
        byte[] wav = new byte[]{
                'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E',
                'f', 'm', 't', ' ', 16, 0, 0, 0, 1, 0, 1, 0,
                0x40, 0x1f, 0, 0, (byte) 0x80, 0x3e, 0, 0, 2, 0, 16, 0,
                'd', 'a', 't', 'a', 0, 0, 0, 0
        };
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", wav);

        String url = service.uploadAudio(1L, file);

        assertTrue(url.startsWith("/audio/manual/"));
        assertTrue(url.endsWith(".wav"));
        assertTrue(Files.exists(audioDir.resolve(url.substring("/audio/".length()))));
        ArgumentCaptor<WordAudio> captor = ArgumentCaptor.forClass(WordAudio.class);
        verify(wordAudioMapper).insert(captor.capture());
        assertEquals(url.substring("/audio/".length()), captor.getValue().getObjectKey());
        assertEquals(wav.length, captor.getValue().getSizeBytes());
    }
}
