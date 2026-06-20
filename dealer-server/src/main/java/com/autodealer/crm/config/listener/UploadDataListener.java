package com.autodealer.crm.config.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.autodealer.crm.config.converter.ClueExcelConverter;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.model.TClue;
import com.autodealer.crm.result.ClueExcel;
import com.autodealer.crm.util.JSONUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

/**
 * 每读一行 Excel 的数据，就会触发该监听器中的 invoke() 方法，Excel 读完之后会触发该监听器中的 doAfterAllAnalysed() 方法
 */
@Slf4j
public class UploadDataListener implements ReadListener<ClueExcel> {

    /**
     * 每隔 100 条存储数据库，实际使用中可以 100 条，然后清理 list ，方便内存回收
     */
    private static final int BATCH_COUNT = 100;

    // 缓存 List
    private List<TClue> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    /**
     * 假设这个是一个 DAO，当然有业务逻辑这个也可以是一个 service。当然如果不用存储这个对象没用。
     */
    private TClueMapper tClueMapper;

    private Integer operatorId;

    private ClueExcelConverter clueExcelConverter;

    /**
     * 如果使用了 spring,请使用这个构造方法。每次创建 Listener 的时候需要把 spring 管理的类传进来
     *
     * @param tClueMapper
     * @param operatorId
     * @param clueExcelConverter
     */
    public UploadDataListener(TClueMapper tClueMapper, Integer operatorId, ClueExcelConverter clueExcelConverter) {
        this.tClueMapper = tClueMapper;
        this.operatorId = operatorId;
        this.clueExcelConverter = clueExcelConverter;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param clueExcel    one row value. It is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(ClueExcel clueExcel, AnalysisContext context) {
        log.info("读取到的每一条数据:{}", JSONUtils.toJSON(clueExcel));

        // 将 ClueExcel 转换为 TClue
        TClue tClue = clueExcelConverter.convertToTClue(clueExcel);

        // 给读到的 clue 对象设置创建时间(导入时间)和创建人（导入人）
        tClue.setCreateTime(new Date());

        tClue.setOwnerId(operatorId);
        tClue.setCreateBy(operatorId);

        //每读取一行，就把该数据放入到一个缓存 List 中
        cachedDataList.add(tClue);

        // 达到 BATCH_COUNT 了，需要去存储一次数据库，防止数据几万条数据在内存，容易 OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            // 把缓存 list 中的数据写入到数据库
            saveData();

            // 存储完成清空 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        log.info("所有数据解析完成！");
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", cachedDataList.size());
        tClueMapper.saveClue(cachedDataList);
        log.info("存储数据库成功！");
    }
}
