package com.msra.avliveness.services;

public interface RecordListener {

    void onStartRecord();

    void onRecordData(byte[] var1);

    void onStopRecord();
}
