// SCR-ORG-01 · Hierarchy setup · FR-ORG-01, 02, 06
import { Network, DoorOpen } from "lucide-react";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

function Node({ label, root = false }: { label: string; root?: boolean }) {
  const Icon = root ? Network : DoorOpen;
  return (
    <div className="flex items-center gap-2 leading-[2.3]">
      <Icon size={14} className="flex-shrink-0 text-slate-dim" strokeWidth={1.8} />
      {root ? <strong>{label}</strong> : label}
    </div>
  );
}

function Children({ children }: { children: React.ReactNode }) {
  return <div className="ml-[26px] border-l border-hairline pl-3.5">{children}</div>;
}

export default function HierarchyPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Hierarchy setup"
        sub={<>Screen <span className="font-mono text-xs">SCR-ORG-01</span> · FR-ORG-01, 02, 06</>}
      />
      <Card>
        <div className="text-[13.5px]">
          <Node label="Main Campus" root />
          <Children>
            <Node label="Science Building" />
            <Children>
              <Node label="Room 101" />
              <Node label="Chemistry Lab" />
              <Node label="Room 204" />
            </Children>
            <Node label="Administration Building" />
            <Children>
              <Node label="Office A10" />
              <Node label="Boardroom" />
            </Children>
            <Node label="Facilities & Storage" />
            <Children>
              <Node label="Storage A" />
              <Node label="Motor Pool" />
            </Children>
          </Children>
        </div>
        <Button variant="secondary" className="mt-4">+ Add node</Button>
      </Card>
    </AnimatedPage>
  );
}
