import * as React from "react";
import { cn } from "@/lib/utils";

const Table = ({ className, ...props }: React.HTMLAttributes<HTMLTableElement>) => (
  <div className="w-full overflow-x-auto">
    <table className={cn("w-full border-collapse text-[13px]", className)} {...props} />
  </div>
);

const Th = ({ className, ...props }: React.ThHTMLAttributes<HTMLTableCellElement>) => (
  <th
    className={cn(
      "border-b border-hairline px-3 py-2.5 text-left text-[11.5px] font-medium uppercase tracking-wide text-slate",
      className
    )}
    {...props}
  />
);

const Td = ({ className, ...props }: React.TdHTMLAttributes<HTMLTableCellElement>) => (
  <td className={cn("border-b border-hairline p-3", className)} {...props} />
);

const Tr = ({ className, ...props }: React.HTMLAttributes<HTMLTableRowElement>) => (
  <tr className={className} {...props} />
);

export { Table, Th, Td, Tr };
