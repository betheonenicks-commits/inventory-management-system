// R2 scope — locked placeholder
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { LockPanel } from "@/components/app/lock-panel";

export default function Page() {
  return (
    <AnimatedPage>
      <PageHeader title="Audit list" />
      <LockPanel name="Audit list" />
    </AnimatedPage>
  );
}
