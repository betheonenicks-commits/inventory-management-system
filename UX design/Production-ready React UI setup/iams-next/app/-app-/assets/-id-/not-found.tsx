import Link from "next/link";
import { LockPanel } from "@/components/app/lock-panel";

export default function AssetNotFound() {
  return (
    <div>
      <LockPanel name="Asset not found — this record" />
      <p className="mt-4 text-center text-[13px]">
        <Link href="/assets" className="text-verify-deep underline">Back to asset list</Link>
      </p>
    </div>
  );
}
