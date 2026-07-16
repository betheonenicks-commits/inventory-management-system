// SCR-AST-01 · Asset list
import Link from "next/link";
import { assets } from "@/lib/data";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { AssetTable } from "@/components/app/asset-table";
import { Card } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";

export default function AssetListPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Asset list"
        sub={<>Screen <span className="font-mono text-xs">SCR-AST-01</span> · click a row to open the record</>}
        actions={
          <Link href="/assets/new" className={buttonVariants()}>
            + Register asset
          </Link>
        }
      />
      <Card className="p-0">
        <AssetTable rows={assets} />
      </Card>
    </AnimatedPage>
  );
}
