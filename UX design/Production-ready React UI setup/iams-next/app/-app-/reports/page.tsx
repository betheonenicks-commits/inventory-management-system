// SCR-RPT-01 · Report catalog · FR-RPT-01 through FR-RPT-10
import { Package, ClipboardCheck, Bell, ArrowLeftRight, Users, FileText, type LucideIcon } from "lucide-react";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";

const reports: { icon: LucideIcon; title: string; desc: string }[] = [
  { icon: Package, title: "Asset register", desc: "Full list, exportable to PDF/Excel/CSV" },
  { icon: ClipboardCheck, title: "Missing / damaged", desc: "Sourced from audit findings" },
  { icon: Bell, title: "Warranty expiry", desc: "30 / 60 / 90 day lookahead" },
  { icon: ArrowLeftRight, title: "Asset movement", desc: "Location and assignment history" },
  { icon: Users, title: "Employee asset list", desc: "Everything assigned to one person" },
  { icon: FileText, title: "Depreciation", desc: "GAAP/IFRS-aligned methods" },
];

export default function ReportCatalogPage() {
  return (
    <AnimatedPage>
      <PageHeader
        title="Report catalog"
        sub={<>Screen <span className="font-mono text-xs">SCR-RPT-01</span> · FR-RPT-01 through FR-RPT-10</>}
      />
      <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 xl:grid-cols-3">
        {reports.map(({ icon: Icon, title, desc }) => (
          <button
            key={title}
            className="rounded-card border border-hairline bg-surface p-[18px] text-left transition-all hover:-translate-y-px hover:border-verify focus-visible:border-verify focus-visible:outline-none"
          >
            <Icon size={16} className="mb-2.5 text-verify" strokeWidth={1.8} />
            <p className="mb-0.5 text-[13.5px] font-medium">{title}</p>
            <p className="text-xs text-slate">{desc}</p>
          </button>
        ))}
      </div>
    </AnimatedPage>
  );
}
