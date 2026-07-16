// SCR-AST-04 · Label print queue · FR-SCN-07
import { assets } from "@/lib/data";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { AssetTable } from "@/components/app/asset-table";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function LabelQueuePage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Label print queue"
        sub={<>Screen <span className="font-mono text-xs">SCR-AST-04</span> · FR-SCN-07 · Code128 / QR, 2&quot;×1&quot; label stock</>}
      />
      <Card className="p-0">
        <AssetTable rows={assets.slice(0, 3)} />
      </Card>
      <Button className="mt-4">Print selected labels</Button>
    </AnimatedPage>
  );
}
