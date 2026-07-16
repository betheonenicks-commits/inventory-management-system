package com.iams.lifecycle.application;

import java.util.UUID;

/**
 * US-NTF-04: published when a transfer is approved or rejected. A plain
 * application event so the lifecycle module stays ignorant of notifications
 * (the notification module already depends on lifecycle for delegation
 * routing - a direct call back would be a package cycle).
 */
public record TransferDecidedEvent(UUID transferId, UUID assetId, String assetName, String decision, String reason,
                                    UUID requesterUserId, UUID fromPersonId, UUID toPersonId, String actorUsername) {
}
