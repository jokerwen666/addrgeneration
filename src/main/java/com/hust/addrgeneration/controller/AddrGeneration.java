package com.hust.addrgeneration.controller;

import com.hust.addrgeneration.beans.InfoBean;
import com.hust.addrgeneration.beans.NormalMsg;
import com.hust.addrgeneration.beans.QueryInfo;
import com.hust.addrgeneration.service.IPv6AddrService;
import com.hust.addrgeneration.service.IPv6Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@CrossOrigin
public class AddrGeneration {
    private final IPv6Service ipv6Service;
    private final IPv6AddrService iPv6AddrService;

    @Autowired
    public AddrGeneration(@Qualifier("IPv6ServiceImpl") IPv6Service iPv6Service, IPv6AddrService iPv6AddrService) {
        this.ipv6Service = iPv6Service;
        this.iPv6AddrService = iPv6AddrService;
    }

    @RequestMapping(value = "/register")
    @ResponseBody
    public NormalMsg register(@RequestBody InfoBean userInfo) throws Exception {
        NormalMsg backHtml = new NormalMsg();
        try {
            String NID = iPv6AddrService.getNID(userInfo);
            backHtml.setStatus(1);
            backHtml.setMessage("用户注册成功！已成功分配NID：" + NID);
            return backHtml;
        } catch (Exception e) {
            backHtml.setStatus(0);
            backHtml.setMessage(e.getMessage());
            return backHtml;
        }
    }

    @RequestMapping(value = "/getIPv6Addr")
    @ResponseBody
    public NormalMsg generateAddr(@RequestBody InfoBean userInfo) throws Exception {
        NormalMsg backHtml = new NormalMsg();
        try {
            String addr = iPv6AddrService.getAddr(userInfo);
            backHtml.setStatus(1);
            backHtml.setMessage("用户注册成功！已成功分配IPv6地址：" + addr);
            return backHtml;
        } catch (Exception e) {
            backHtml.setStatus(0);
            if (e.getMessage() == null)
                backHtml.setMessage("用户尚未注册！请先注册");
            else
                backHtml.setMessage(e.getMessage());
            return backHtml;
        }
    }

    @RequestMapping(value = "/query")
    @ResponseBody
    public NormalMsg queryAddr(@RequestBody InfoBean userInfo) throws Exception {
        QueryInfo backHtml = new QueryInfo();
        try {
            backHtml = iPv6AddrService.queryAddr(userInfo);
            backHtml.setStatus(1);
            backHtml.setMessage("地址查询成功");
            return backHtml;
        } catch (Exception e) {
            backHtml.setStatus(0);
            if (e.getMessage() == null)
                backHtml.setMessage("查询地址不存在！");
            else
                backHtml.setMessage(e.getMessage());
            return backHtml;
        }
    }
}
