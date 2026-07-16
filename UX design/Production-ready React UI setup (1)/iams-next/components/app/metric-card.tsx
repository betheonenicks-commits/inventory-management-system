import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface MetricCardProps {
  label: string;
  value: string | number;
  delta?: string;
  deltaTone?: "up" | "warn" | "neutral";
}

export function MetricCard({ label, value, delta, deltaTone = "neutral" }: MetricCardProps) {
  return (
    <Card className="px-[18px] py-4">
      <p className="mb-2 text-[12.5px] text-slate">{label}</p>
      <p className="font-mono text-[26px] font-medium tracking-tight">{value}</p>
      {delta && (
        <p
          className={cn("mt-1.5 text-[11.5px]", {
            "text-verify": deltaTone === "up",
            "text-rust": deltaTone === "warn",
            "text-slate": deltaTone === "neutral",
          })}
        >
          {delta}
        </p>
      )}
    </Card>
  );
}
