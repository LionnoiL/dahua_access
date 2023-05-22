package ua.gaponov.dahua;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.module.BaseModule;
import com.sun.jna.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.gaponov.database.DataBase;
import ua.gaponov.database.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.netsdk.lib.NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;
import static com.netsdk.lib.NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
import static ua.gaponov.conf.Config.*;

public class Dahua {

    private static final Logger LOG = LoggerFactory.getLogger(Dahua.class);
    private final DataBase dataBase = new DataBase();
    private final BaseModule baseModule;
    private final NetSDKLib netSdkApi = NetSDKLib.NETSDK_INSTANCE;
    NetSDKLib.NET_DEVICEINFO_Ex info;
    NetSDKLib.LLong loginId;

    public Dahua() {
        baseModule = new BaseModule(netSdkApi);
    }

    public boolean init() {
        return baseModule.init(
                DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
    }

    public void login() {
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY stuIn = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();
        stuIn.emSpecCap = EM_LOGIN_SPEC_CAP_TCP;
        stuIn.pCapParam = null;
        stuIn.nPort = 37777;
        stuIn.szUserName = DAHUA_USERNAME.getBytes();
        stuIn.szPassword = DAHUA_PASSWORD.getBytes();
        stuIn.szIP = DAHUA_IP.getBytes();

        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY stuOut = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();
        loginId = netSdkApi.CLIENT_LoginWithHighLevelSecurity(stuIn, stuOut);

        if (loginId == new NetSDKLib.LLong()) {
            LOG.error("login failed. {}", ENUMERROR.getErrorMessage());
            return;
        }
        LOG.info("login");
        info = stuOut.stuDeviceInfo;
    }

    public void logout() {
        netSdkApi.CLIENT_Logout(loginId);
        LOG.info("logout");
    }

    public boolean findRecords() {
        final int nFindCount = 10;//в пакете всегда

        //++параметры поиска
        NetSDKLib.NET_IN_FIND_RECORD_PARAM stIn = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
        stIn.emType = NET_RECORD_ACCESSCTLCARDREC_EX;

        NetSDKLib.NET_OUT_FIND_RECORD_PARAM stOut = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();
        //--параметры поиска

        //пустой массив ответа
        NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecordEx;

        //начало поиска
        if (netSdkApi.CLIENT_FindRecord(loginId, stIn, stOut, 5000)) {

            //этот контроллер отдает по 10 шт всегда. не зависимо от даты
            for (int k = 0; k < COUNT_RECORD_IN_BATCH; k++) {
                pstRecordEx = getNext(nFindCount, stOut);
                save(pstRecordEx);
            }
            //завершение поиска
            netSdkApi.CLIENT_FindRecordClose(stOut.lFindeHandle);
        }
        return true;
    }

    public NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] getNext(int nFindCount, NetSDKLib.NET_OUT_FIND_RECORD_PARAM stOut) {
        //пустой массив ответа
        NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecordEx = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[0];

        //создание массива записей
        NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecord = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[nFindCount];

        //заполнение массива записей пустыми
        for (int i = 0; i < nFindCount; i++) {
            pstRecord[i] = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC();
        }

        //++параметры следующей записи
        NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stNextIn = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
        stNextIn.lFindeHandle = stOut.lFindeHandle;
        stNextIn.nFileCount = nFindCount;

        NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stNextOut = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
        stNextOut.nMaxRecordNum = nFindCount;
        stNextOut.pRecordList = new Memory((long) pstRecord[0].dwSize * nFindCount);
        stNextOut.pRecordList.clear((long) pstRecord[0].dwSize * nFindCount);
        //--параметры следующей записи

        ToolKits.SetStructArrToPointerData(pstRecord, stNextOut.pRecordList);

        //получение следующей порции
        if (netSdkApi.CLIENT_FindNextRecord(stNextIn, stNextOut, 10000)) {
            if (stNextOut.nRetRecordNum == 0) {
                return pstRecordEx;
            }
            ToolKits.GetPointerDataToStructArr(stNextOut.pRecordList, pstRecord);
            pstRecordEx = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[stNextOut.nRetRecordNum];
            System.arraycopy(pstRecord, 0, pstRecordEx, 0, stNextOut.nRetRecordNum);
        }
        return pstRecordEx;
    }

    public void save(NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] cardRecords) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<Event> events = new ArrayList<>();
        for (NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC cardRecord : cardRecords) {
            String cardNo = new String(cardRecord.szCardNo).trim();
            if (cardNo.equals("00000000")) {
                continue;
            }

            LOG.info("{} {}", cardNo, cardRecord.stuTime);
            try {
                Date docDate = format.parse(cardRecord.stuTime.toString());
                Date actualDate = Date.from(docDate.toInstant().plus(Duration.ofHours(2)));
                String text = format2.format(actualDate);
                events.add(new Event(cardNo, text));
            } catch (ParseException e) {
                LOG.error("Error parse date");
            }
        }
        dataBase.saveEvents(events);
    }
}
