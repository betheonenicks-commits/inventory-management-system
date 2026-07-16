package com.iams.asset.application;

import java.util.UUID;

/** US-NTF-04: published on assign/unassign so the notification module can inform the affected person's account. */
public record AssetAssignmentChangedEvent(UUID assetId, String assetNumber, String assetName, UUID personId,
                                           String personName, String action, String actorUsername) {
}
