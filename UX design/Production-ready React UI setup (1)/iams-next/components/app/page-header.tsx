import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  /** Screen code + FR references, e.g. "Screen SCR-AST-01 · FR-AST-01" */
  sub?: ReactNode;
  actions?: ReactNode;
}

export function PageHeader({ title, sub, actions }: PageHeaderProps) {
  return (
    <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="m-0 text-[21px] font-semibold tracking-tight">{title}</h1>
        {sub && <p className="mt-1 text-[13px] text-slate">{sub}</p>}
      </div>
      {actions}
    </div>
  );
}
