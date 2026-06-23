package it.albemiglio.accounts.core.services;

public interface Coordinator extends TaskQueue {

    String getInstanceId();

    boolean trySetLeader();

    String getLeader();

    void clearLeader();

    void extendLeaderTTL();
}
