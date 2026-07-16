// SCR-SEC-01 · Activity log · FR-SEC-04
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { StatusPill } from "@/components/app/status-pill";
import { Card } from "@/components/ui/card";
import { Table, Th, Td, Tr } from "@/components/ui/table";

const events = [
  { time: "09:14", user: "m.reyes", event: "Login success", alert: false },
  { time: "09:02", user: "unknown", event: "Login failed ×3", alert: true },
  { time: "08:55", user: "p.chen", event: "Exported asset register", alert: false },
];

export default function ActivityLogPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Activity log"
        sub={<>Screen <span className="font-mono text-xs">SCR-SEC-01</span> · FR-SEC-04</>}
      />
      <Card className="p-0">
        <Table>
          <thead>
            <tr><Th>Time</Th><Th>User</Th><Th>Event</Th></tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <Tr key={e.time}>
                <Td className="font-mono text-xs">{e.time}</Td>
                <Td>{e.user}</Td>
                <Td>
                  {e.alert && <StatusPill status="Missing" />} {e.event}
                </Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      </Card>
    </AnimatedPage>
  );
}
