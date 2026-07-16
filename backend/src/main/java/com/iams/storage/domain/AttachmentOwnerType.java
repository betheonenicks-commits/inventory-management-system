package com.iams.storage.domain;

/**
 * What kind of entity an attachment belongs to. US-PLAT-02 names three binary
 * families (asset attachments, audit evidence, signature records); only audit
 * evidence has a consuming story built so far (US-AUD-11) - add members here
 * as the others ship rather than pre-declaring speculative ones.
 */
public enum AttachmentOwnerType {
    AUDIT_FINDING
}
