package com.hust.addrgeneration.serviceImpl;

import com.hust.addrgeneration.beans.InfoBean;
import com.hust.addrgeneration.beans.QueryInfo;
import com.hust.addrgeneration.beans.QueryKeyInfo;
import com.hust.addrgeneration.dao.UserMapper;
import com.hust.addrgeneration.service.IPv6AddrService;
import com.hust.addrgeneration.utils.ConvertUtils;
import com.hust.addrgeneration.utils.EncDecUtils;
import com.hust.addrgeneration.utils.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IPv6AddrServiceImpl implements IPv6AddrService {
    private final UserMapper userMapper;

    @Autowired
    public IPv6AddrServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public String getNID(InfoBean infoBean) {
        String userID = infoBean.getUserID();
        String password = infoBean.getPassword();
        String phoneNumber = infoBean.getPhoneNumber();

        // step1. Calculate NID with user's information
        String encryptStr = userID + phoneNumber;
        String hashStr = HashUtils.SM3Hash(encryptStr);
        String userPart = ConvertUtils.hexStringToBinString(hashStr).substring(0,38);
        String userType = userID.substring(0,1);
        String organizationPart = "";
        switch (userType) {
            case "U" :
                organizationPart = "00";
                break;

            case "M" :
                organizationPart = "01";
                break;

            case "D" :
                organizationPart = "10";
                break;
            default:
                organizationPart = "11";
                break;
        }

        String NID = ConvertUtils.binStringToHexString(userPart + organizationPart);
        userMapper.register(NID,password,userID,phoneNumber);

        return NID;

    }

    @Override
    public String getAddr(InfoBean infoBean) throws Exception {
        String NID = infoBean.getNid();
        String password = infoBean.getPassword();

        // step1. check NID and password
        /*
         if the NID isn't in the database, return the information tells user to register a NID
         if the NID isn't match the password, return the wrong password information
         */
        String passwordFromDB = userMapper.getKey(NID);
        if (!passwordFromDB.equals(password)) {
            throw new Exception("?????????????????????????????????");
        }


        // step2. Calculate the time information
        LocalDateTime localDateTime1 = LocalDateTime.now();
        LocalDateTime localDateTime2 = LocalDateTime.of(localDateTime1.getYear(), 1, 1, 0, 0, 0);

        long currentTime = localDateTime1.toEpochSecond(ZoneOffset.of("+8"));
        long baseTime = localDateTime2.toEpochSecond(ZoneOffset.of("+8"));

        /*
         the raw time difference maybe over 24-bits, so we use proper degree of accuracy to change the time difference into a proper format
         the degrees of accuracy can be 30s 10s or 5s
         the max value of time difference is 31,536,000, the max value of 2???-bits is 16,777,215
         so every degree can make the value of time difference smaller than the max value of 24-bits
         */
        int timeDifference = (int) (( currentTime - baseTime ) / 10);
        String timeInformation = ConvertUtils.decToBinString(timeDifference, 24);
        String rawAID = NID + ConvertUtils.binStringToHexString(timeInformation);

        // step3. Generate AID-noTimeHash(aka AID_nTH) with UID and time information
        String preAID = EncDecUtils.ideaEncrypt(rawAID, EncDecUtils.ideaKey);
        String str1 = preAID.substring(0,16);
        String str2 = preAID.substring(16,32);

        BigInteger big1 = new BigInteger(str1, 16);
        BigInteger big2 = new BigInteger(str2, 16);
        String AIDnTH = big1.xor(big2).toString(16);
        userMapper.updateAID(AIDnTH, big1.toString(16));

        // step4. Generate AID-withTimeHash(aka AID) with AIDnTH and time-Hash
        LocalDateTime localDateTime3 = LocalDateTime.of(localDateTime1.getYear(),localDateTime1.getMonth(),localDateTime1.getDayOfMonth(),localDateTime1.getHour(),0,0);
        long nearestTimeHour = localDateTime3.toEpochSecond(ZoneOffset.of("+8"));
        int timeDifference2 = (int) (nearestTimeHour - baseTime);
        String timeHash = EncDecUtils.md5Encrypt16(ConvertUtils.decToHexString(timeDifference2,10));

        BigInteger big3 = new BigInteger(AIDnTH,16);
        BigInteger big4 = new BigInteger(timeHash, 16);
        String AID = big3.xor(big4).toString(16);
        userMapper.updateTimeHash(AID, AIDnTH);

        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < AID.length(); i+=4) {
            suffix.append(AID, i, i + 4).append(":");
        }
        String asPrefix = "2001:250:4000:511a:";
        return asPrefix + suffix.substring(0,suffix.length()-1);
    }

    @Override
    public QueryInfo queryAddr(InfoBean infoBean) throws Exception {
        // step1. use prefix of the IPv6-address and calculate time-Hash to get key
        String queryAddress = infoBean.getQueryAddress();
        int pos = getIndexOf(queryAddress, ":", 4);
        String asPrefix = queryAddress.substring(0,pos);
        String asAddress = asPrefix + "::1";
        String aidStr = queryAddress.substring(pos+1);
        String AID = aidStr.replace(":","");
        String AIDnTH = userMapper.getAIDnTH(AID);

        BigInteger big1 = new BigInteger(AID, 16);
        BigInteger big2 = new BigInteger(AIDnTH, 16);
        String timeHash = big1.xor(big2).toString(16);
        String ideaKey = userMapper.getIdeaKey(asAddress, timeHash);
        if (ideaKey == null)
            throw new Exception("????????????????????????????????????");

        // step2. use suffix of IPv6-address to get the whole encrypt data(128-bits)

        String prefix = userMapper.getPrefix(AIDnTH);
        BigInteger big3 = new BigInteger(AIDnTH, 16);
        BigInteger big4 = new BigInteger(prefix, 16);
        String suffix = big3.xor(big4).toString(16);
        String preAID = prefix + suffix;

        // step3. use the proper key to decrypt the encrypt data(128-bits)
        String rawAID = EncDecUtils.ideaDecrypt(preAID, ideaKey);
        if (rawAID == null || rawAID.length() != 16)
            throw new Exception("???????????????");

        // step4. use the NID to query user information the return the info(userID, phoneNumber, address-generate-time etc.) to user
        String NID = rawAID.substring(0,10);
        String timeInfoStr = ConvertUtils.hexStringToBinString(rawAID.substring(10));
        int timeInfo = Integer.parseInt(timeInfoStr, 2) * 10;
        LocalDateTime localDateTime2 = LocalDateTime.of(LocalDate.now().getYear(), 1, 1, 0, 0, 0);
        long baseTime = localDateTime2.toEpochSecond(ZoneOffset.of("+8"));
        long registerTime = (baseTime + timeInfo);

        Instant instant = Instant.ofEpochSecond(registerTime);
        LocalDateTime registerTimeStr = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        QueryInfo queryInfo = userMapper.queryAddrInfo(NID);
        queryInfo.setRegisterTime(registerTimeStr.toString());
        return queryInfo;
    }

    private int getIndexOf(String data, String str, int num) {
        Pattern pattern = Pattern.compile(str);
        Matcher matcher = pattern.matcher(data);

        int indexNum = 0;

        while (matcher.find()) {
            indexNum++;
            if (indexNum == num)
                break;
        }

        return matcher.start();
    }
}
