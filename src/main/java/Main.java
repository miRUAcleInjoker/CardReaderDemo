import java.util.Arrays;
import java.util.Scanner;

public class Main {
    /*初始化设备*/
    private static long g_hDevice[] = new long[]{-1}; //hDevice must init as -1
    private static int status;

    public static void main(String[] args) {
        open();
        selectCard();
        readCard();
        writeCard();
        readCard();
    }


    public static void open() {
        boolean isOpen;
        //=================== Connect the reader ===================
        //Check whether the reader is connected or not
        //If the reader is already open , close it firstly
        isOpen = ReaderLib.INSTANCE.Sys_IsOpen(g_hDevice[0]);
        if (isOpen == true) {
            status = ReaderLib.INSTANCE.Sys_Close(g_hDevice);
            if (status != 0) {
                System.out.println("Sys_Close failed!");
                return;
            }
        }

        //Connect
        status = ReaderLib.INSTANCE.Sys_Open(g_hDevice, 0, (short) 0x0416, (short) 0x8020);
        if (status != 0) {
            System.out.println("Sys_Open failed !");
            return;
        }

        //========== Init the reader before operating the card ==========
        //Close antenna of the reader
        status = ReaderLib.INSTANCE.Sys_SetAntenna(g_hDevice[0], (byte) 0);
        if (status != 0) {
            System.out.println("Sys_SetAntenna failed !");
            return;
        }
        //Appropriate delay after Sys_SetAntenna operating
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }
        //Set the reader's working mode
        status = ReaderLib.INSTANCE.Sys_InitType(g_hDevice[0], (byte) 'A');
        if (status != 0) {
            System.out.println("Sys_InitType failed !");
            return;
        }
        //Appropriate delay after Sys_SetAntenna operating
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        //Open antenna of the reader
        status = ReaderLib.INSTANCE.Sys_SetAntenna(g_hDevice[0], (byte) 1);
        if (status != 0) {
            System.out.println("Sys_SetAntenna failed !");
            return;
        }
        //Appropriate delay after Sys_SetAntenna operating
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        //==================== Success Tips ====================
        //Beep 200 ms
//		        status = ReaderLib.INSTANCE.Sys_SetBuzzer(g_hDevice[0], (byte)20);
        if (status != 0) {
            System.out.println("Sys_SetBuzzer failed !");
            return;
        }

        //Tips
        System.out.println("Connect reader succeed !");
    }

    /**
     * 读卡
     * 要对感应区内的M1卡(4字节卡号)进行读写操作应顺序调用以下库函数:
     * <p>
     * 1. TyA_Request;
     * <p>
     * 2. TyA_Anticollision;
     * <p>
     * 3. TyA_Select;
     * <p>
     * 此后卡片处于激活状态。
     */
    public static void selectCard() {
        byte mode = 0x52;
        short[] TagType = new short[1];
        byte bcnt = 0;
        byte snr[] = new byte[16];
        byte len[] = new byte[1];
        byte sak[] = new byte[1];

        //Check whether the reader is connected or not
        if (false == ReaderLib.INSTANCE.Sys_IsOpen(g_hDevice[0])) {
            System.out.println("Please connect the device firstly !");
            return;
        }

        //Request card
        status = ReaderLib.INSTANCE.TyA_Request(g_hDevice[0], mode, TagType);//search all card
        if (status != 0) {
            System.out.println("TyA_Request failed !");
            return;
        }
        //Anticollision
        status = ReaderLib.INSTANCE.TyA_Anticollision(g_hDevice[0], bcnt, snr, len);//return serial number of card
        if (status != 0 || len[0] != 4) {
            System.out.println("TyA_Anticollision failed !");
            return;
        }
        String str = "";
        for (int i = 0; i < 4; i++) {
            str = str + String.format("%02X", snr[i]);
        }
        System.out.println("UID=" + str);
        System.out.println("original_UID=" + Arrays.toString(snr));

        //Select card
        status = ReaderLib.INSTANCE.TyA_Select(g_hDevice[0], snr, len[0], sak);//lock ISO14443-3 TYPE_A
        if (status != 0) {
            System.out.println("TyA_Select failed !");
            return;
        }

        //Tips
        System.out.println("Request card succeed !");
    }

    public static void readCard() {
        byte mode = 0x60;               //mode = 0x60 -> 验证A密钥；mode = 0x61 -> 验证B密钥；
        byte secnr = 0x00;              //绝对块号，分为16个扇区，每个扇区4个块，每块16个字节，以块为存取单位
        byte cLen[] = new byte[1];      //返回数据的长度
        byte pData[] = new byte[64];    //返回的数据
        byte key[] = new byte[6];       //密钥内容，6 字节

        //Get key type;这里默认0x60
        /**/

        //Get key
        String strKey = "FFFFFFFFFFFF";     //默认密钥：000000
        for (int i = 0; i < 6; i++) {
            byte value = (byte) Integer.parseInt(strKey.substring(2 * i, 2 * i + 2), 16);
            key[i] = value;
        }

        //Get block address
        System.out.println("输入绝对块号(0~63)");
        secnr = new Scanner(System.in).nextByte();

        //Check whether the reader is connected or not
        if (false == ReaderLib.INSTANCE.Sys_IsOpen(g_hDevice[0])) {
            System.out.println("Please connect the device firstly !");
            return;
        }

        //Authentication
        status = ReaderLib.INSTANCE.TyA_CS_Authentication2(g_hDevice[0], mode, (byte) ((secnr / 4) * 4), key);
        if (status != 0) {
            System.out.println("TyA_CS_Authentication2 failed !");
            return;
        }

        //Read card
        status = ReaderLib.INSTANCE.TyA_CS_Read(g_hDevice[0], secnr, pData, cLen);
        if (status != 0 || cLen[0] != 16) {
            System.out.println("TyA_CS_Read failed !");
            return;
        }
        //改变灯光
        status = ReaderLib.INSTANCE.Sys_SetLight(g_hDevice[0], (byte) 2);      //变绿 1s
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        status = ReaderLib.INSTANCE.Sys_SetLight(g_hDevice[0], (byte) 1);       //变红

        //处理pData:读出的数据
        String str = "";
        for (int i = 0; i < 16; i++) {
            str = str + String.format("%02X", pData[i]);
        }
        System.out.println(secnr + "块上的数据=" + str);
    }

    private static void writeCard() {
        byte mode = 0x60;
        byte secnr = 0x00;
        byte cLen[] = new byte[1];
        byte arrData[] = new byte[64];
        byte key[] = new byte[6];

        //Get key type
        /*这里默认是0x60*/

        //Get key
        String strKey = "FFFFFFFFFFFF";     //默认密钥：000000
        for (int i = 0; i < 6; i++) {
            byte value = (byte) Integer.parseInt(strKey.substring(2 * i, 2 * i + 2), 16);
            key[i] = value;
        }

        //Get block address
        System.out.println("输入绝对块号(0~63)");
        secnr = new Scanner(System.in).nextByte();

        //输入要写入的数据
        System.out.println("输入要写入的数据：");
        String strData = new Scanner(System.in).nextLine();
        if (strData.length() != 32) {
            System.out.println("Please input 16 bytes data in data area !");
            return;
        }
        for (int i = 0; i < 16; i++) {
            byte value = (byte) Integer.parseInt(strData.substring(2 * i, 2 * i + 2), 16);
            arrData[i] = value;
        }

        //Check whether the reader is connected or not
        if (false == ReaderLib.INSTANCE.Sys_IsOpen(g_hDevice[0])) {
            System.out.println("Please connect the device firstly !");
            return;
        }

        //Authentication
        status = ReaderLib.INSTANCE.TyA_CS_Authentication2(g_hDevice[0], mode, (byte) ((secnr / 4) * 4), key);
        if (status != 0) {
            System.out.println("TyA_CS_Authentication2 failed !");
            return;
        }

        //Write card
        status = ReaderLib.INSTANCE.TyA_CS_Write(g_hDevice[0], secnr, arrData);
        if (status != 0) {
            System.out.println("TyA_CS_Write failed !");
            return;
        }
        System.out.println("Write card succeed !");
    }
}
