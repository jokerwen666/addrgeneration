package com.hust.addrgeneration.service;

import com.alibaba.fastjson.JSONObject;
import com.hust.addrgeneration.beans.InfoBean;
import com.hust.addrgeneration.beans.QueryInfo;
import org.springframework.stereotype.Service;

public interface IPv6AddrService {
    public String getNID(InfoBean infoBean);
    public String getAddr(InfoBean infoBean) throws Exception;
    public QueryInfo queryAddr(InfoBean infoBean) throws Exception;
}
