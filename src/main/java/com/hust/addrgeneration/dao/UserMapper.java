package com.hust.addrgeneration.dao;

import com.hust.addrgeneration.beans.KeyInfo;
import com.hust.addrgeneration.beans.QueryKeyInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMapper {
    int register(String NID, String password, String userID, String phoneNumber);
    int updateKey(KeyInfo keyInfo);
    int updateAID(String aid, String prefix);
    String getKey(String NID);
    String getPrefix(String AID);
    List<QueryKeyInfo> getIdeaKey(String addrGenIP);
}
