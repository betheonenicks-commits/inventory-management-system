"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard, Package, Tag, Boxes, ArrowLeftRight, ClipboardCheck,
  FileText, Network, Users, Shield, ShieldCheck, Plug, type LucideIcon,
} from "lucide-react";
import { navigation } from "@/lib/data";
import { Seal } from "./seal";
import { cn } from "@/lib/utils";

const icons: Record<string, LucideIcon> = {
  LayoutDashboard, Package, Tag, Boxes, ArrowLeftRight, ClipboardCheck,
  FileText, Network, Users, Shield, ShieldCheck, Plug,
};

export function Sidebar() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Primary"
      className="flex h-full flex-col overflow-y-auto bg-ink px-3.5 py-5 text-white"
    >
      <Link href="/dashboard" className="flex items-center gap-2.5 px-2 pb-6">
        <Seal size={22} />
        <span className="text-[15px] font-semibold tracking-tight">IAMS</span>
      </Link>

      {navigation.map((group) => (
        <div key={group.label}>
          <p className="px-2.5 pb-1.5 pt-4 font-mono text-[10px] uppercase tracking-wider text-ink-dim">
            {group.label}
          </p>
          {group.items.map((item) => {
            const active = pathname.startsWith(item.href);
            const Icon = icons[item.icon];
            return (
              <Link
                key={item.id}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "mb-px flex items-center gap-2.5 rounded-[7px] px-2.5 py-2 text-[13.5px] text-[#C7CCDA] transition-colors hover:bg-ink-2 hover:text-white",
                  active && "bg-ink-2 font-medium text-white"
                )}
              >
                {active && <span className="-ml-[13px] mr-[3px] h-4 w-[3px] rounded-sm bg-gold" />}
                {Icon && <Icon size={16} strokeWidth={1.8} className="flex-shrink-0 opacity-85" />}
                <span>{item.name}</span>
                {!item.r1 && (
                  <span className="ml-auto rounded-full border border-ink-border bg-ink-2 px-1.5 py-px font-mono text-[9px] text-ink-dim">
                    R2
                  </span>
                )}
              </Link>
            );
          })}
        </div>
      ))}

      <div className="mt-auto border-t border-ink-border px-2.5 pb-1 pt-3.5 text-[11px] text-ink-dim">
        Signed in as
        <br />
        <span className="font-mono text-[#C7CCDA]">elena.m@example.org</span>
      </div>
    </nav>
  );
}
