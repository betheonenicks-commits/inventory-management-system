import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-1.5 rounded-field text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-verify focus-visible:ring-offset-2 focus-visible:ring-offset-paper disabled:pointer-events-none disabled:opacity-50 active:scale-[0.98]",
  {
    variants: {
      variant: {
        primary: "bg-verify text-white hover:bg-verify-deep dark:text-ink",
        secondary: "bg-surface text-foreground border border-hairline hover:border-slate-dim",
        ghost: "text-slate hover:bg-paper hover:text-foreground",
      },
      size: {
        default: "px-4 py-2.5",
        sm: "px-3 py-1.5 text-[13px]",
        icon: "h-[34px] w-[34px]",
      },
    },
    defaultVariants: { variant: "primary", size: "default" },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button ref={ref} className={cn(buttonVariants({ variant, size }), className)} {...props} />
  )
);
Button.displayName = "Button";

export { Button, buttonVariants };
