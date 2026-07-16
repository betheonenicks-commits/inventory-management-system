import type { ActivityEvent } from "@/lib/types";
import { cn } from "@/lib/utils";

export function ActivityFeed({ items }: { items: ActivityEvent[] }) {
  return (
    <ul className="m-0 list-none p-0">
      {items.map((item, i) => (
        <li
          key={i}
          className="flex gap-2.5 border-b border-hairline py-2.5 text-[12.5px] last:border-none"
        >
          <span
            className={cn(
              "mt-1.5 h-1.5 w-1.5 flex-shrink-0 rounded-full",
              item.severity === "alert" ? "bg-rust" : "bg-verify"
            )}
          />
          <div>
            <p className="m-0">{item.text}</p>
            <span className="text-[11px] text-slate-dim">{item.time}</span>
          </div>
        </li>
      ))}
    </ul>
  );
}
