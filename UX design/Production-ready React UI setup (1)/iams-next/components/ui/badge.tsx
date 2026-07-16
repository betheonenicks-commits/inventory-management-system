import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11.5px] font-medium",
  {
    variants: {
      tone: {
        verify: "bg-verify-dim text-verify-deep",
        amber: "bg-amber-dim text-amber",
        rust: "bg-rust-dim text-rust",
        slate: "border border-hairline bg-paper text-slate",
      },
    },
    defaultVariants: { tone: "slate" },
  }
);

const dotColor: Record<string, string> = {
  verify: "bg-verify",
  amber: "bg-amber",
  rust: "bg-rust",
  slate: "bg-slate-dim",
};

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {
  dot?: boolean;
}

function Badge({ className, tone, dot = true, children, ...props }: BadgeProps) {
  return (
    <span className={cn(badgeVariants({ tone }), className)} {...props}>
      {dot && <span className={cn("h-1.5 w-1.5 rounded-full", dotColor[tone ?? "slate"])} />}
      {children}
    </span>
  );
}

export { Badge, badgeVariants };
