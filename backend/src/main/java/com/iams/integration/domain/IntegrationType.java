package com.iams.integration.domain;

/**
 * FR-INT registry integration kinds. Only kinds the product actually names are listed;
 * add one as its own integration lands. {@code needsCredential} marks a kind whose
 * configuration must carry a secrets-manager reference (US-SEC-15) - a read-only export
 * that authenticates outbound needs one; a purely inbound webhook receiver may not.
 */
public enum IntegrationType {
    /** FR-INT-01: read-only accounting/ERP export - authenticates outbound, so a credential is required. */
    ACCOUNTING_EXPORT(true),
    /** FR-INT-02: HR/SIS roster pull - authenticates to the source system. */
    HR_ROSTER_SYNC(true),
    /** FR-INT-03: identity-provider (LDAP/AD, SAML2/OIDC) - a bind password / client secret is required. */
    IDENTITY_PROVIDER(true),
    /** FR-INT-04/06: outbound webhook with a per-webhook signing secret. */
    OUTBOUND_WEBHOOK(true);

    private final boolean needsCredential;

    IntegrationType(boolean needsCredential) {
        this.needsCredential = needsCredential;
    }

    public boolean needsCredential() {
        return needsCredential;
    }

    public static boolean isValid(String name) {
        if (name == null) {
            return false;
        }
        for (IntegrationType t : values()) {
            if (t.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
