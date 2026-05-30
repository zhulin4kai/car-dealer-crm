package com.bjpowernode.config.converter;

import com.bjpowernode.model.TClue;
import com.bjpowernode.result.ClueExcel;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class ClueExcelConverter {

    public TClue convertToTClue(ClueExcel clueExcel) {
        TClue tClue = new TClue();
        BeanUtils.copyProperties(clueExcel, tClue);
        return tClue;
    }
}
