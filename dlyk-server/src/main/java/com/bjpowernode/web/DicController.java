package com.bjpowernode.web;

import com.bjpowernode.DlykServerApplication;
import com.bjpowernode.model.TActivity;
import com.bjpowernode.model.TDicValue;
import com.bjpowernode.model.TProduct;
import com.bjpowernode.result.DicEnum;
import com.bjpowernode.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DicController {

    @GetMapping(value = "/api/dicvalue/{typeCode}")
    public R dicData(@PathVariable(value = "typeCode") String typeCode) {
        if (typeCode.equals(DicEnum.ACTIVITY.getCode())) { //activity
            List<TActivity> tActivityList = (List<TActivity>)DlykServerApplication.cacheMap.get(typeCode);
            return R.OK(tActivityList);
        } else if (typeCode.equals(DicEnum.PRODUCT.getCode())) {
            List<TProduct> tProductList = (List<TProduct>)DlykServerApplication.cacheMap.get(typeCode);
            return R.OK(tProductList);
        } else {
            List<TDicValue> tDicValueList = (List<TDicValue>)DlykServerApplication.cacheMap.get(typeCode);
            return R.OK(tDicValueList);
        }
    }
}
