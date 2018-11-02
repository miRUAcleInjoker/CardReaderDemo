import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

public interface ReaderLib extends StdCallLibrary {
    ReaderLib INSTANCE = (ReaderLib) Native.loadLibrary("src/main/resources/dll/hfrdapi", ReaderLib.class);

    int Sys_Open(long[] device, int index, short vid, short pid);

    int Sys_Close(long[] device);

    boolean Sys_IsOpen(long device);

    int Sys_SetLight(long device, byte color);

    int Sys_SetBuzzer(long device, byte msec);

    int Sys_SetAntenna(long device, byte mode);

    int Sys_InitType(long device, byte type);

    int TyA_Request(long device, byte mode, short[] pTagType);

    int TyA_Anticollision(long device, byte bcnt, byte[] pSnr, byte[] pLen);

    int TyA_Select(long device, byte[] pSnr, byte snrLen, byte[] pSak);

    int TyA_Halt(long device);

    int TyA_CS_Authentication2(long device, byte mode, byte block, byte[] pKey);

    int TyA_CS_Read(long device, byte block, byte[] pData, byte[] pLen);

    int TyA_CS_Write(long device, byte block, byte[] pData);

    int TyA_UID_Write(long device, byte[] pData);
}
