package it.albemiglio.accounts.core.services;

import java.util.Set;

/**
 * The set of accounts instances currently alive (proxy + backends), maintained by heartbeats. A
 * migration snapshots this at initiation as the set that must report done before it is complete, so a
 * server that is permanently dead doesn't hold the barrier open forever.
 */
public interface InstanceRegistry {

    Set<String> activeInstances();
}
