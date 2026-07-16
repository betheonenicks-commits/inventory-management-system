// SCR-CMP-01 · Retention policy · FR-CMP-01
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { Card } from "@/components/ui/card";
import { Table, Th, Td, Tr } from "@/components/ui/table";

const policies = [
  { entity: "Login logs", period: "1 year", action: "Delete" },
  { entity: "Former employee records", period: "2 years", action: "Anonymize" },
  { entity: "Audit evidence", period: "Indefinite", action: "Legal hold eligible" },
];

export default function RetentionPolicyPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Retention policy"
        sub={<>Screen <span className="font-mono text-xs">SCR-CMP-01</span> · FR-CMP-01</>}
      />
      <Card className="p-0">
        <Table>
          <thead>
            <tr><Th>Entity type</Th><Th>Retention period</Th><Th>Action at expiry</Th></tr>
          </thead>
          <tbody>
            {policies.map((p) => (
              <Tr key={p.entity}>
                <Td>{p.entity}</Td><Td>{p.period}</Td><Td>{p.action}</Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      </Card>
    </AnimatedPage>
  );
}
