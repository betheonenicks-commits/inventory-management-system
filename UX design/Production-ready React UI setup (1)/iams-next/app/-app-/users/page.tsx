// SCR-USR-01 · Users · FR-USR-01 through FR-USR-05
import { users } from "@/lib/data";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { StatusPill } from "@/components/app/status-pill";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Table, Th, Td, Tr } from "@/components/ui/table";

export default function UsersPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Users"
        sub={<>Screen <span className="font-mono text-xs">SCR-USR-01</span> · FR-USR-01 through FR-USR-05</>}
        actions={<Button>+ Invite user</Button>}
      />
      <Card className="p-0">
        <Table>
          <thead>
            <tr>
              <Th>Name</Th>
              <Th>Role</Th>
              <Th className="hidden md:table-cell">Department</Th>
              <Th>Status</Th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <Tr key={u.email}>
                <Td>
                  {u.name}
                  <br />
                  <span className="text-[11.5px] text-slate-dim">{u.email}</span>
                </Td>
                <Td>
                  <span className="rounded-full border border-hairline bg-paper px-2 py-0.5 text-[11px] text-slate">
                    {u.role}
                  </span>
                </Td>
                <Td className="hidden md:table-cell">{u.department}</Td>
                <Td>
                  <StatusPill status={u.status} />
                  {u.pendingReturns ? (
                    <span className="ml-1.5 text-[11px] text-rust">
                      {u.pendingReturns} asset pending return
                    </span>
                  ) : null}
                </Td>
              </Tr>
            ))}
          </tbody>
        </Table>
      </Card>
    </AnimatedPage>
  );
}
