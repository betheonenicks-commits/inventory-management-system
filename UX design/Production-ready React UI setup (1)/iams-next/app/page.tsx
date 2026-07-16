"use client";

// SCR-GLB-01 · FR-SEC-01, FR-SEC-02
// Login is a visual scaffold: it routes straight to /dashboard.
// Productionize by wiring these handlers to next-auth (see README).

import { useRouter } from "next/navigation";
import { motion, useReducedMotion } from "framer-motion";
import { Seal } from "@/components/app/seal";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function LoginPage() {
  const router = useRouter();
  const reduce = useReducedMotion();

  return (
    <div className="grid min-h-screen grid-cols-1 lg:grid-cols-2">
      {/* Brand hero panel */}
      <div className="relative hidden flex-col justify-center overflow-hidden bg-ink p-20 text-white lg:flex">
        <div
          aria-hidden="true"
          className="absolute -bottom-44 -right-44 h-[480px] w-[480px] rounded-full border border-ink-border"
        />
        <div
          aria-hidden="true"
          className="absolute -bottom-20 -right-20 h-[300px] w-[300px] rounded-full border border-ink-border"
        />
        <div className="mb-8">
          <Seal size={46} />
        </div>
        <h1 className="mb-3 text-3xl font-semibold leading-tight tracking-tight">
          Every asset accounted for.
          <br />
          Every audit provable.
        </h1>
        <p className="mb-10 max-w-[360px] text-[15px] leading-relaxed text-ink-dim">
          IAMS is the on-premises system of record for physical assets and audits — built so
          nothing has to be taken on trust.
        </p>
        <dl className="flex gap-10">
          {[
            ["41", "screens mapped"],
            ["16", "modules"],
            ["100%", "on-premises"],
          ].map(([value, label]) => (
            <div key={label}>
              <dd className="m-0 font-mono text-[22px] tracking-tight">{value}</dd>
              <dt className="mt-0.5 text-xs text-ink-dim">{label}</dt>
            </div>
          ))}
        </dl>
      </div>

      {/* Sign-in form */}
      <div className="flex flex-col items-center justify-center p-8">
        <motion.form
          initial={reduce ? false : { opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, ease: "easeOut" }}
          className="w-full max-w-[340px]"
          onSubmit={(e) => {
            e.preventDefault();
            router.push("/dashboard");
          }}
        >
          <div className="mb-6 lg:hidden">
            <Seal size={38} />
          </div>
          <h2 className="mb-1.5 text-xl font-semibold tracking-tight">Sign in to IAMS</h2>
          <p className="mb-8 text-[13px] text-slate">Main Campus deployment</p>

          <div className="mb-4">
            <Label htmlFor="email">Email or username</Label>
            <Input id="email" type="text" defaultValue="elena.m@example.org" autoComplete="username" />
          </div>
          <div className="mb-4">
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" defaultValue="0000000000" autoComplete="current-password" />
          </div>

          <Button type="submit" className="w-full">Sign in</Button>

          <div className="my-[18px] flex items-center gap-2.5 text-xs text-slate-dim before:h-px before:flex-1 before:bg-hairline after:h-px after:flex-1 after:bg-hairline">
            or
          </div>

          <Button type="button" variant="secondary" className="w-full">Sign in with SSO</Button>

          <p className="mt-[22px] text-center text-xs text-slate-dim">
            Screen SCR-GLB-01 · FR-SEC-01, FR-SEC-02
          </p>
        </motion.form>
      </div>
    </div>
  );
}
