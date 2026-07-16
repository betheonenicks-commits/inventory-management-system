// SCR-GLB-03 · Notifications · FR-NTF-03
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { ActivityFeed } from "@/components/app/activity-feed";
import { Card } from "@/components/ui/card";

export default function NotificationsPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Notifications"
        sub={<>Screen <span className="font-mono text-xs">SCR-GLB-03</span> · FR-NTF-03</>}
      />
      <Card>
        <ActivityFeed
          items={[
            { text: "3 assets have warranty expiring within 30 days", time: "Today" },
            { text: "AST-00701 marked Missing during last audit", time: "3 days ago", severity: "alert" },
            { text: "J. Ferreira's record is eligible for anonymization review", time: "4 days ago" },
          ]}
        />
      </Card>
    </AnimatedPage>
  );
}
