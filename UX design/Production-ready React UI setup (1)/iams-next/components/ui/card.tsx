import * as React from "react";
import { cn } from "@/lib/utils";

const Card = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn("rounded-card border border-hairline bg-surface p-5 shadow-[0_1px_2px_rgba(20,24,33,0.05),0_4px_14px_rgba(20,24,33,0.05)]", className)}
      {...props}
    />
  )
);
Card.displayName = "Card";

const CardTitle = React.forwardRef<HTMLHeadingElement, React.HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => (
    <h3 ref={ref} className={cn("mb-4 text-sm font-semibold", className)} {...props} />
  )
);
CardTitle.displayName = "CardTitle";

export { Card, CardTitle };
