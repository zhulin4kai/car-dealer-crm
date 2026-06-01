package com.autodealer.crm.config.converter;

import com.autodealer.crm.model.TClue;
import com.autodealer.crm.result.ClueExcel;
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
