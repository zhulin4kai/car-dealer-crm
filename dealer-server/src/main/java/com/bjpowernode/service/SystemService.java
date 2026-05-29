package com.bjpowernode.service;

import com.bjpowernode.model.TSystem;
import java.util.List;

public interface SystemService {
    List<TSystem> getAllList();
    TSystem getById(Integer id);
    void create(TSystem system);
    void update(Integer id, TSystem system);
    void delete(Integer id);
    void batchDelete(List<Integer> ids);
    void toggleStatus(Integer id, String isOpen);
}
