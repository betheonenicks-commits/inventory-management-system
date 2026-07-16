// SCR-DSH-01 · Executive dashboard
import { assets, recentActivity } from "@/lib/data";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { MetricCard } from "@/components/app/metric-card";
import { ActivityFeed } from "@/components/app/activity-feed";
import { Card, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export default function DashboardPage() {
  const count = (s: string) => assets.filter((a) => a.status === s).length;
  const bars = [
    { label: "In use", value: count("In use"), cls: "bg-verify" },
    { label: "Storage", value: count("In storage"), cls: "bg-verify-dim" },
    { label: "Repair", value: count("Under repair"), cls: "bg-verify-dim" },
    { label: "Missing", value: count("Missing"), cls: "bg-rust-dim" },
  ];

  return (
    <AnimatedPage>
      <PageHeader
        title="Executive dashboard"
        sub={<>Screen <span className="font-mono text-xs">SCR-DSH-01</span> · demo dataset · {assets.length} assets</>}
      />

      <div className="mb-6 grid grid-cols-2 gap-3.5 xl:grid-cols-4">
        <MetricCard label="Total assets" value={assets.length} delta="Main Campus" />
        <MetricCard label="In use" value={count("In use")} delta="On track" deltaTone="up" />
        <MetricCard label="Missing" value={count("Missing")} delta="Needs review" deltaTone="warn" />
        <MetricCard label="Warranty expiring (60d)" value={1} delta="Epson projector" deltaTone="warn" />
      </div>

      <div className="grid grid-cols-1 gap-[18px] xl:grid-cols-[1.4fr_1fr]">
        <Card>
          <CardTitle>Assets by status</CardTitle>
          <div className="flex h-[140px] items-end gap-3.5 pt-2.5" role="img" aria-label="Bar chart of assets by status">
            {bars.map((b) => (
              <div key={b.label} className="flex flex-1 flex-col items-center gap-2">
                <div
                  className={cn("w-full max-w-[34px] rounded-t", b.cls)}
                  style={{ height: b.value * 28 }}
                />
                <span className="font-mono text-[11px] text-slate">{b.value}</span>
                <span className="text-center text-[11px] text-slate">{b.label}</span>
              </div>
            ))}
          </div>
        </Card>
        <Card>
          <CardTitle>Recent activity</CardTitle>
          <ActivityFeed items={recentActivity} />
        </Card>
      </div>
    </AnimatedPage>
  );
}
