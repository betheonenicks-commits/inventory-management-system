import * as React from "react";
import { cn } from "@/lib/utils";

const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "w-full rounded-field border border-hairline bg-surface px-3 py-2.5 text-sm text-foreground placeholder:text-slate-dim transition-colors focus:border-gold focus:ring-2 focus:ring-gold-dim focus:outline-none",
        className
      )}
      {...props}
    />
  )
);
Input.displayName = "Input";

export { Input };
