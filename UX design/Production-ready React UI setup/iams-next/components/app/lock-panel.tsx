import { Lock } from "lucide-react";
import { Card } from "@/components/ui/card";

/** Placeholder for screens defined in the IA but outside Release R1 scope. */
export function LockPanel({ name }: { name: string }) {
  return (
    <Card className="p-12 text-center">
      <Lock className="mx-auto mb-3.5 h-8 w-8 text-slate-dim" strokeWidth={1.6} />
      <p className="mb-1 text-sm font-medium">{name} is scoped for a later release</p>
      <p className="text-[12.5px] text-slate">
        This screen is defined in the Information Architecture but out of Release R1 (MVP) scope.
      </p>
    </Card>
  );
}
