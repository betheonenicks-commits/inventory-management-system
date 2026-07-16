"use client";

import { useRouter } from "next/navigation";
import type { Asset } from "@/lib/types";
import { Table, Th, Td, Tr } from "@/components/ui/table";
import { StatusPill } from "./status-pill";

/** Clickable asset table — rows navigate to the asset record. */
export function AssetTable({ rows }: { rows: Asset[] }) {
  const router = useRouter();
  return (
    <Table>
      <thead>
        <tr>
          <Th>Asset</Th>
          <Th className="hidden md:table-cell">Category</Th>
          <Th>Location</Th>
          <Th className="hidden md:table-cell">Assigned to</Th>
          <Th>Status</Th>
        </tr>
      </thead>
      <tbody>
        {rows.map((a) => (
          <Tr
            key={a.id}
            role="link"
            tabIndex={0}
            onClick={() => router.push(`/assets/${a.id}`)}
            onKeyDown={(e) => e.key === "Enter" && router.push(`/assets/${a.id}`)}
            className="cursor-pointer transition-colors hover:bg-paper focus-visible:bg-paper focus-visible:outline-none"
          >
            <Td>
              <span className="font-mono text-[12.5px] text-verify-deep">{a.id}</span>
              <br />
              <span className="text-[13px]">{a.name}</span>
            </Td>
            <Td className="hidden md:table-cell">{a.category}</Td>
            <Td>{a.location}</Td>
            <Td className="hidden md:table-cell">{a.assignee ?? "—"}</Td>
            <Td>
              <StatusPill status={a.status} />
            </Td>
          </Tr>
        ))}
      </tbody>
    </Table>
  );
}
