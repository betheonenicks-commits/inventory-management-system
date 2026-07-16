// SCR-AST-02 · Asset record
import Link from "next/link";
import { notFound } from "next/navigation";
import { assets } from "@/lib/data";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { StatusPill } from "@/components/app/status-pill";
import { ActivityFeed } from "@/components/app/activity-feed";
import { Card, CardTitle } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";

export function generateStaticParams() {
  return assets.map((a) => ({ id: a.id }));
}

export default function AssetDetailPage({ params }: { params: { id: string } }) {
  const asset = assets.find((a) => a.id === params.id);
  if (!asset) notFound();

  const details: [string, React.ReactNode][] = [
    ["Category", asset.category],
    ["Location", asset.location],
    ["Department", asset.department],
    ["Assigned to", asset.assignee ?? "—"],
    ["Status", <StatusPill key="s" status={asset.status} />],
    ["Vendor", asset.vendor],
    ["Purchase cost", `$${asset.cost}`],
    ["Warranty expiry", asset.warranty ?? "—"],
    ...Object.entries(asset.fields),
  ];

  return (
    <AnimatedPage>
      <PageHeader
        title={asset.name}
        sub={<><span className="font-mono text-xs">{asset.id}</span> · Screen <span className="font-mono text-xs">SCR-AST-02</span></>}
        actions={
          <Link href="/assets/new" className={buttonVariants({ variant: "secondary" })}>
            Edit
          </Link>
        }
      />
      <div className="grid grid-cols-1 gap-[18px] xl:grid-cols-[1.4fr_1fr]">
        <Card>
          <CardTitle>Details</CardTitle>
          <table className="w-full border-collapse text-[13px]">
            <tbody>
              {details.map(([k, v]) => (
                <tr key={k}>
                  <td className="border-b border-hairline p-3 text-slate">{k}</td>
                  <td className="border-b border-hairline p-3">{v}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
        <Card>
          <CardTitle>History</CardTitle>
          <ActivityFeed
            items={[
              { text: "Registered", time: asset.purchased },
              { text: `Assigned to ${asset.department}`, time: asset.purchased },
              { text: `Status set to ${asset.status}`, time: "Most recent" },
            ]}
          />
        </Card>
      </div>
    </AnimatedPage>
  );
}
