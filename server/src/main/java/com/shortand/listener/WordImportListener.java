package com.shortand.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.shortand.domain.vo.WordImportVo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WordImportListener extends AnalysisEventListener<WordImportVo> {
    private static final int BATCH_SIZE = 500;
    private final Consumer<List<WordImportVo>> batchConsumer;
    private final List<WordImportVo> batch = new ArrayList<>();
    private int total;

    public WordImportListener(Consumer<List<WordImportVo>> batchConsumer) {
        this.batchConsumer = batchConsumer;
    }

    @Override
    public void invoke(WordImportVo data, AnalysisContext context) {
        total++;
        data.setRowNum(context.readRowHolder().getRowIndex() + 1);
        batch.add(data);
        if (batch.size() >= BATCH_SIZE) flush();
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flush();
    }

    public int getTotal() {
        return total;
    }

    private void flush() {
        if (!batch.isEmpty()) {
            batchConsumer.accept(new ArrayList<>(batch));
            batch.clear();
        }
    }
}
