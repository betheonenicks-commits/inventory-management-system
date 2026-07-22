package com.iams.migration.domain;

/**
 * The record types a bulk import can target (US-MIG-01 names Asset, Person,
 * Vendor, Inventory Item). Only ASSET has an executable importer in this first
 * EPIC-MIG slice; the others are declared so the template/enum surface is stable
 * as they're built, and are refused with a clear message until then.
 */
public enum ImportEntityType {
    ASSET,
    PERSON,
    VENDOR,
    INVENTORY_ITEM
}
