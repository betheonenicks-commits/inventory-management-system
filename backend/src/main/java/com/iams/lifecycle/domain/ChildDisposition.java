package com.iams.lifecycle.domain;

/**
 * US-AST-04: when a parent asset is transferred or disposed, each child must be
 * explicitly dispositioned before the action can complete. The "block" option
 * the story names is enforced by requiring a disposition for every child at
 * request time - a child left without one blocks the request - so only the two
 * apply-able choices are modeled here.
 */
public enum ChildDisposition {
    /** The child follows the parent: same new org node on transfer, disposed/retired alongside on disposal. */
    MOVE_WITH_PARENT,
    /** The child is unlinked from the parent and left as-is, becoming a standalone asset. */
    DETACH
}
