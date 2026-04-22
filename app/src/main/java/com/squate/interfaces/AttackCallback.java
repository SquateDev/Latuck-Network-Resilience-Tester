package com.squate.interfaces;

public interface AttackCallback {
    void onStatusUpdate(String status);
    void onStatsUpdate(int packets, int connections);
}
