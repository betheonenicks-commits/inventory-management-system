import { Badge } from "@/components/ui/badge";

const toneMap: Record<string, "verify" | "amber" | "rust" | "slate"> = {
  "In use": "verify",
  "In storage": "slate",
  "Under repair": "amber",
  Missing: "rust",
  Active: "verify",
  Offboarding: "amber",
  "Pending Delivery": "amber",
  Received: "verify",
};

/** Maps any domain status string to the correct badge tone. */
export function StatusPill({ status }: { status: string }) {
  return <Badge tone={toneMap[status] ?? "slate"}>{status}</Badge>;
}
