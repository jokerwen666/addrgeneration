package com.hust.addrgeneration.beans;

import org.springframework.stereotype.Component;

@Component
public class KeyInfo {
    private String addrGenIP;
    private String ideaKey;
    private int timeInfo;

    public String getAddrGenIP() {
        return addrGenIP;
    }

    public void setAddrGenIP(String addrGenIP) {
        this.addrGenIP = addrGenIP;
    }

    public String getIdeaKey() {
        return ideaKey;
    }

    public void setIdeaKey(String ideaKey) {
        this.ideaKey = ideaKey;
    }

    public int getTimeInfo() {
        return timeInfo;
    }

    public void setTimeInfo(int timeInfo) {
        this.timeInfo = timeInfo;
    }
}
