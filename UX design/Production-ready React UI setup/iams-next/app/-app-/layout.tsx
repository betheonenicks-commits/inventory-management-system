import { Sidebar } from "@/components/app/sidebar";
import { Topbar } from "@/components/app/topbar";

/**
 * Authenticated app shell: fixed sidebar + topbar + scrollable content.
 * Wrap this route group with session middleware when adding real auth.
 */
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid h-screen grid-cols-[64px_1fr] md:grid-cols-[232px_1fr]">
      <div className="hidden md:block">
        <Sidebar />
      </div>
      {/* Compact sidebar on tablet: same nav, icons only */}
      <div className="block md:hidden">
        <Sidebar />
      </div>
      <div className="flex min-w-0 flex-col">
        <Topbar />
        <main className="flex-1 overflow-y-auto px-4 pb-16 pt-6 md:px-8 md:pt-7">{children}</main>
      </div>
    </div>
  );
}
