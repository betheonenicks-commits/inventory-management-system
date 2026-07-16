"use client";

import Link from "next/link";
import { Bell, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "./theme-toggle";

export function Topbar({ breadcrumb }: { breadcrumb?: string }) {
  return (
    <header className="flex flex-shrink-0 items-center gap-4 border-b border-hairline bg-surface px-4 py-3 md:px-7">
      {breadcrumb && <p className="hidden text-[13px] text-slate lg:block">{breadcrumb}</p>}

      <div className="ml-0 flex max-w-[360px] flex-1 items-center gap-2 rounded-field border border-hairline px-3 py-[7px] text-[13px] text-slate-dim lg:ml-3">
        <Search size={14} aria-hidden="true" />
        <input
          type="search"
          placeholder="Search assets, people, orders"
          aria-label="Global search"
          className="w-full border-none bg-transparent text-[13px] text-foreground outline-none placeholder:text-slate-dim"
        />
      </div>

      <div className="flex-1" />

      <ThemeToggle />

      <Button variant="ghost" size="icon" aria-label="Notifications" asChild={false} className="relative">
        <Link href="/notifications" className="flex h-full w-full items-center justify-center">
          <Bell size={17} strokeWidth={1.8} />
          <span className="absolute right-[7px] top-1.5 h-[7px] w-[7px] rounded-full border-[1.5px] border-surface bg-rust" />
        </Link>
      </Button>

      <div
        aria-label="Elena Marsh"
        className="flex h-[30px] w-[30px] items-center justify-center rounded-full bg-verify-dim font-mono text-[11px] font-semibold text-verify-deep"
      >
        EM
      </div>
    </header>
  );
}
