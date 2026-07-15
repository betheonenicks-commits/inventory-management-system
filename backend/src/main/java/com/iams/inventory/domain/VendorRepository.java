package com.iams.inventory.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByOrderByNameAsc();

    /** US-SRC-01: the vendor leg of global search - capped, since a global search shows a preview, not a register. */
    List<Vendor> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
