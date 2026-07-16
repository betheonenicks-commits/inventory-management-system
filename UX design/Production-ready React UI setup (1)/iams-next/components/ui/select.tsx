import * as React from "react";
import { cn } from "@/lib/utils";

/** Native select, styled to match Input. Swap for Radix Select if richer behavior is needed. */
const Select = React.forwardRef<HTMLSelectElement, React.SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, ...props }, ref) => (
    <select
      ref={ref}
      className={cn(
        "w-full rounded-field border border-hairline bg-surface px-3 py-2.5 text-sm text-foreground transition-colors focus:border-gold focus:ring-2 focus:ring-gold-dim focus:outline-none",
        className
      )}
      {...props}
    />
  )
);
Select.displayName = "Select";

export { Select };
